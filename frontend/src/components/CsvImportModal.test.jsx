import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import CsvImportModal from './CsvImportModal'
import { importTransactions } from '../api/transactions'

vi.mock('../api/transactions', () => ({
  importTransactions: vi.fn(),
}))

const VALID_CSV = `symbol,txType,quantity,price,date,buyPrice
AAPL,BUY,10,187.32,2026-01-15,
MSFT,SELL,5,412.50,2026-02-03,398.10
`

const MISSING_BUY_PRICE_CSV = `symbol,txType,quantity,price,date,buyPrice
MSFT,SELL,5,412.50,2026-02-03,
`

const MISSING_HEADER_CSV = `symbol,txType,price,date
AAPL,BUY,187.32,2026-01-15
`

function getFileInput(container) {
  return container.querySelector('input[type="file"]')
}

function makeCsvFile(content, name = 'transactions.csv') {
  return new File([content], name, { type: 'text/csv' })
}

describe('CsvImportModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders nothing when closed', () => {
    const { container } = render(<CsvImportModal open={false} onClose={vi.fn()} onImported={vi.fn()} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('previews parsed rows and marks them valid', async () => {
    const user = userEvent.setup()
    const { container } = render(<CsvImportModal open onClose={vi.fn()} onImported={vi.fn()} />)

    await user.upload(getFileInput(container), makeCsvFile(VALID_CSV))

    expect(await screen.findByText('AAPL')).toBeInTheDocument()
    expect(screen.getByText('MSFT')).toBeInTheDocument()
    expect(screen.getAllByText('Valid')).toHaveLength(2)
    expect(screen.getByText('2 of 2 row(s) valid')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /import 2 transaction/i })).toBeEnabled()
  })

  it('shows a header error when a required column is missing', async () => {
    const user = userEvent.setup()
    const { container } = render(<CsvImportModal open onClose={vi.fn()} onImported={vi.fn()} />)

    await user.upload(getFileInput(container), makeCsvFile(MISSING_HEADER_CSV))

    expect(await screen.findByText(/missing column\(s\): quantity/i)).toBeInTheDocument()
  })

  it('flags a SELL row missing buyPrice as invalid', async () => {
    const user = userEvent.setup()
    const { container } = render(<CsvImportModal open onClose={vi.fn()} onImported={vi.fn()} />)

    await user.upload(getFileInput(container), makeCsvFile(MISSING_BUY_PRICE_CSV))

    expect(await screen.findByText(/buyprice is required/i)).toBeInTheDocument()
    expect(screen.getByText('0 of 1 row(s) valid')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /import 0 transaction/i })).toBeDisabled()
  })

  it('imports only the valid rows and reports the imported count', async () => {
    importTransactions.mockResolvedValue([{ id: 1 }, { id: 2 }])
    const onImported = vi.fn()
    const user = userEvent.setup()
    const { container } = render(<CsvImportModal open onClose={vi.fn()} onImported={onImported} />)

    await user.upload(getFileInput(container), makeCsvFile(VALID_CSV))
    await screen.findByText('AAPL')
    await user.click(screen.getByRole('button', { name: /import 2 transaction/i }))

    expect(await screen.findByText(/imported 2 transaction\(s\)/i)).toBeInTheDocument()
    expect(importTransactions).toHaveBeenCalledTimes(1)
    const submittedRows = importTransactions.mock.calls[0][0]
    expect(submittedRows).toHaveLength(2)
    expect(submittedRows[0]).toMatchObject({ symbol: 'AAPL', txType: 'BUY', quantity: 10, price: 187.32 })
    expect(submittedRows[1]).toMatchObject({ symbol: 'MSFT', txType: 'SELL', quantity: 5, buyPrice: 398.1 })
    expect(onImported).toHaveBeenCalledTimes(1)
  })
})
