import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import EditTransactionModal from './EditTransactionModal'
import { getOpenLots } from '../api/transactions'

vi.mock('../api/transactions', () => ({
  getOpenLots: vi.fn(),
}))

const buyTransaction = {
  id: 5,
  symbol: 'AAPL',
  txType: 'BUY',
  quantity: 10,
  price: 150,
  buyTransactionId: null,
  executedAt: '2025-01-15T00:00:00Z',
}

const sellTransaction = {
  id: 9,
  symbol: 'AAPL',
  txType: 'SELL',
  quantity: 4,
  price: 200,
  buyTransactionId: 5,
  executedAt: '2025-06-01T00:00:00Z',
}

describe('EditTransactionModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders nothing when there is no transaction', () => {
    const { container } = render(
      <EditTransactionModal transaction={null} onSave={vi.fn()} onClose={vi.fn()} saving={false} />
    )
    expect(container).toBeEmptyDOMElement()
  })

  it('pre-fills the form fields from the given transaction', () => {
    render(<EditTransactionModal transaction={buyTransaction} onSave={vi.fn()} onClose={vi.fn()} saving={false} />)

    expect(screen.getByLabelText(/symbol/i)).toHaveValue('AAPL')
    expect(screen.getByLabelText(/quantity/i)).toHaveValue(10)
    expect(screen.getByLabelText(/^price$/i)).toHaveValue(150)
  })

  it('fetches open lots (excluding itself) and pre-selects the existing lot for a SELL transaction', async () => {
    getOpenLots.mockResolvedValue([
      { transactionId: 5, price: 150, executedAt: '2025-01-15T00:00:00Z', remainingQuantity: 6 },
    ])
    render(<EditTransactionModal transaction={sellTransaction} onSave={vi.fn()} onClose={vi.fn()} saving={false} />)

    await waitFor(() => expect(getOpenLots).toHaveBeenCalledWith('AAPL', 9))
    expect(await screen.findByRole('option', { name: /6 avail/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/buy price/i)).toHaveValue('5')
  })

  it('shows a validation error when quantity exceeds the selected lot remaining amount', async () => {
    getOpenLots.mockResolvedValue([
      { transactionId: 5, price: 150, executedAt: '2025-01-15T00:00:00Z', remainingQuantity: 6 },
    ])
    const onSave = vi.fn()
    const user = userEvent.setup()
    render(<EditTransactionModal transaction={sellTransaction} onSave={onSave} onClose={vi.fn()} saving={false} />)

    await screen.findByRole('option', { name: /6 avail/i })
    await user.clear(screen.getByLabelText(/quantity/i))
    await user.type(screen.getByLabelText(/quantity/i), '20')
    await user.click(screen.getByRole('button', { name: /save changes/i }))

    expect(await screen.findByText(/only 6 share\(s\) available in this lot/i)).toBeInTheDocument()
    expect(onSave).not.toHaveBeenCalled()
  })

  it('submits the updated values for a BUY transaction', async () => {
    const onSave = vi.fn()
    const user = userEvent.setup()
    render(<EditTransactionModal transaction={buyTransaction} onSave={onSave} onClose={vi.fn()} saving={false} />)

    await user.clear(screen.getByLabelText(/quantity/i))
    await user.type(screen.getByLabelText(/quantity/i), '12')
    await user.click(screen.getByRole('button', { name: /save changes/i }))

    await waitFor(() => expect(onSave).toHaveBeenCalledTimes(1))
    const [id, payload] = onSave.mock.calls[0]
    expect(id).toBe(5)
    expect(payload).toMatchObject({ symbol: 'AAPL', txType: 'BUY', quantity: 12, price: 150 })
    expect(payload.buyTransactionId).toBeUndefined()
  })

  it('calls onClose when Cancel is clicked', async () => {
    const onClose = vi.fn()
    const user = userEvent.setup()
    render(<EditTransactionModal transaction={buyTransaction} onSave={vi.fn()} onClose={onClose} saving={false} />)

    await user.click(screen.getByRole('button', { name: /cancel/i }))

    expect(onClose).toHaveBeenCalledTimes(1)
  })
})
