import './DataTable.css'

export default function TablePagination({ page, pageSize, totalItems, onPageChange }) {
  const totalPages = Math.max(1, Math.ceil((totalItems ?? 0) / pageSize))

  const current = Math.min(Math.max(page, 1), totalPages)

  return (
    <nav className="table-pagination" aria-label="Table pagination">
      <button
        type="button"
        className="btn btn-ghost table-page-btn"
        onClick={() => onPageChange(current - 1)}
        disabled={current <= 1}
      >
        Prev
      </button>

      <p className="table-page-status">
        Page {current} of {totalPages}
      </p>

      <button
        type="button"
        className="btn btn-ghost table-page-btn"
        onClick={() => onPageChange(current + 1)}
        disabled={current >= totalPages}
      >
        Next
      </button>
    </nav>
  )
}