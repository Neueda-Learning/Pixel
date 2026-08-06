import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import TransactionForm from './TransactionForm'
import { getOpenLots } from '../api/transactions'
import { searchSymbols } from '../api/market'

vi.mock('../api/transactions', () => ({
  getOpenLots: vi.fn(),
}))

vi.mock('../api/market', () => ({
  searchSymbols: vi.fn(),
}))

/** Types into the symbol autocomplete, waits for the debounced search, and picks the first result. */
async function pickInstrument(user, symbol = 'AAPL', description = 'Apple Inc') {
  const input = screen.getByPlaceholderText(/search company or ticker/i)
  await user.type(input, symbol)
  const label = await screen.findByText(description)
  await user.click(label.closest('button'))
}

describe('TransactionForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    searchSymbols.mockResolvedValue([{ symbol: 'AAPL', description: 'Apple Inc' }])
  })

  it('does not show the buy-lot picker for a BUY transaction', () => {
    render(<TransactionForm onSubmit={vi.fn()} submitting={false} />)
    expect(screen.queryByLabelText(/buy price/i)).not.toBeInTheDocument()
  })

  it('shows the buy-lot picker once SELL is selected', async () => {
    const user = userEvent.setup()
    render(<TransactionForm onSubmit={vi.fn()} submitting={false} />)

    await user.selectOptions(screen.getByLabelText(/type/i), 'SELL')

    expect(screen.getByLabelText(/buy price/i)).toBeInTheDocument()
  })

  it('fetches and lists open lots for the selected symbol when selling', async () => {
    getOpenLots.mockResolvedValue([
      { transactionId: 1, price: 150, executedAt: '2025-01-15T00:00:00Z', remainingQuantity: 10 },
    ])
    const user = userEvent.setup()
    render(<TransactionForm onSubmit={vi.fn()} submitting={false} />)

    await user.selectOptions(screen.getByLabelText(/type/i), 'SELL')
    await pickInstrument(user)

    await waitFor(() => expect(getOpenLots).toHaveBeenCalledWith('AAPL'))
    expect(await screen.findByRole('option', { name: /10 avail/i })).toBeInTheDocument()
  })

  it('requires a lot to be selected before submitting a SELL', async () => {
    getOpenLots.mockResolvedValue([
      { transactionId: 1, price: 150, executedAt: '2025-01-15T00:00:00Z', remainingQuantity: 10 },
    ])
    const onSubmit = vi.fn();
    const user = userEvent.setup()
    render(<TransactionForm onSubmit={onSubmit} submitting={false} />)

    await user.selectOptions(screen.getByLabelText(/type/i), 'SELL')
    await pickInstrument(user)
    await screen.findByRole('option', { name: /10 avail/i })
    await user.type(screen.getByLabelText(/quantity/i), '5')
    await user.type(screen.getByLabelText(/sell price/i), '200')
    await user.click(screen.getByRole('button', { name: /add transaction/i }))

    expect(await screen.findByText(/select which buy lot/i)).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('rejects a quantity greater than the selected lot\'s remaining amount', async () => {
    getOpenLots.mockResolvedValue([
      { transactionId: 1, price: 150, executedAt: '2025-01-15T00:00:00Z', remainingQuantity: 10 },
    ])
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<TransactionForm onSubmit={onSubmit} submitting={false} />)

    await user.selectOptions(screen.getByLabelText(/type/i), 'SELL')
    await pickInstrument(user)
    await screen.findByRole('option', { name: /10 avail/i })
    await user.selectOptions(screen.getByLabelText(/buy price/i), '1')
    await user.type(screen.getByLabelText(/quantity/i), '15')
    await user.type(screen.getByLabelText(/sell price/i), '200')
    await user.click(screen.getByRole('button', { name: /add transaction/i }))

    expect(await screen.findByText(/only 10 share\(s\) available in this lot/i)).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits a valid SELL with the chosen buyTransactionId and resets the form', async () => {
    getOpenLots.mockResolvedValue([
      { transactionId: 1, price: 150, executedAt: '2025-01-15T00:00:00Z', remainingQuantity: 10 },
    ])
    const onSubmit = vi.fn().mockResolvedValue()
    const user = userEvent.setup()
    render(<TransactionForm onSubmit={onSubmit} submitting={false} />)

    await user.selectOptions(screen.getByLabelText(/type/i), 'SELL')
    await pickInstrument(user)
    await screen.findByRole('option', { name: /10 avail/i })
    await user.selectOptions(screen.getByLabelText(/buy price/i), '1')
    await user.type(screen.getByLabelText(/quantity/i), '5')
    await user.type(screen.getByLabelText(/sell price/i), '200')
    await user.click(screen.getByRole('button', { name: /add transaction/i }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    const payload = onSubmit.mock.calls[0][0]
    expect(payload).toMatchObject({ symbol: 'AAPL', txType: 'SELL', quantity: 5, price: 200, buyTransactionId: 1 })
  })

  it('submits a valid BUY without a buyTransactionId', async () => {
    const onSubmit = vi.fn().mockResolvedValue()
    const user = userEvent.setup()
    render(<TransactionForm onSubmit={onSubmit} submitting={false} />)

    await pickInstrument(user)
    await user.type(screen.getByLabelText(/quantity/i), '10')
    await user.type(screen.getByLabelText(/^price$/i), '150')
    await user.click(screen.getByRole('button', { name: /add transaction/i }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    const payload = onSubmit.mock.calls[0][0]
    expect(payload).toMatchObject({ symbol: 'AAPL', txType: 'BUY', quantity: 10, price: 150 })
    expect(payload.buyTransactionId).toBeUndefined()
  })
})
