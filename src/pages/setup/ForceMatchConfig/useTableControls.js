import { useState, useMemo, useCallback, useRef, useEffect } from 'react';

/**
 * Shared hook for table sort + debounced search.
 * Eliminates duplicate sort/filter logic across tabs.
 */
const useTableControls = (data, searchKeys, debounceMs = 250) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedTerm, setDebouncedTerm] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });
  const timerRef = useRef(null);

  // Debounce search input
  useEffect(() => {
    timerRef.current = setTimeout(() => setDebouncedTerm(searchTerm), debounceMs);
    return () => clearTimeout(timerRef.current);
  }, [searchTerm, debounceMs]);

  const handleSearch = useCallback((value) => {
    setSearchTerm(value);
  }, []);

  const clearSearch = useCallback(() => {
    setSearchTerm('');
    setDebouncedTerm('');
  }, []);

  const handleSort = useCallback((key) => {
    setSortConfig((prev) => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
    }));
  }, []);

  const filtered = useMemo(() => {
    if (!debouncedTerm) return data;
    const term = debouncedTerm.toLowerCase();
    return data.filter((item) =>
      searchKeys.some((key) => String(item[key] ?? '').toLowerCase().includes(term))
    );
  }, [data, debouncedTerm, searchKeys]);

  const sorted = useMemo(() => {
    if (!sortConfig.key) return filtered;
    return [...filtered].sort((a, b) => {
      const aVal = a[sortConfig.key] ?? '';
      const bVal = b[sortConfig.key] ?? '';
      if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filtered, sortConfig]);

  return {
    searchTerm,
    handleSearch,
    clearSearch,
    sortConfig,
    handleSort,
    filtered,
    sorted,
    totalCount: data.length,
    filteredCount: filtered.length,
  };
};

export default useTableControls;
