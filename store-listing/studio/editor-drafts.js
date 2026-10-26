/** Keep unsaved copy when moving between projects. Disk files remain the source for saved coverage. */
export function createDraftStore(scope, storage = sessionStorage) {
  const key = locale => `studio-draft:${scope}:${locale}`;
  return {
    restore(locale, disk) {
      const saved = JSON.stringify(disk);
      try {
        const cached = JSON.parse(storage.getItem(key(locale)) || 'null');
        if (!cached || !cached.data || typeof cached.data !== 'object' || Array.isArray(cached.data)) return {data: disk, saved, saving: false, error: '', conflict: false};
        if (JSON.stringify(cached.data) === saved) { storage.removeItem(key(locale)); return {data: disk, saved, saving: false, error: '', conflict: false}; }
        return {data: cached.data, saved, saving: false, error: '', conflict: cached.base !== saved, restoreBase: cached.base};
      } catch { return {data: disk, saved, saving: false, error: '', conflict: false}; }
    },
    persist(locale, draft) {
      if (JSON.stringify(draft.data) === draft.saved) storage.removeItem(key(locale));
      else storage.setItem(key(locale), JSON.stringify({data: draft.data, base: draft.conflict ? draft.restoreBase : draft.saved}));
    }
  };
}
