import { useCallback, useEffect, useState } from 'react'

/** Minimal data-fetching hook: loading/error/data + a manual reload, re-runs when `deps` change. */
export default function useApi(fetcher, deps = []) {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    fetcher()
      .then((d) => setData(d))
      .catch((e) => setError(e))
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  useEffect(() => {
    load()
  }, [load])

  return { data, error, loading, reload: load }
}
