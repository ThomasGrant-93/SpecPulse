interface PaginationProps {
    page: number;
    totalPages: number;
    totalItems: number;
    pageSize: number;
    onPageChange: (page: number) => void;
    onPageSizeChange: (size: number) => void;
    pageSizeOptions?: number[];
    itemName?: string;
}

/**
 * Reusable Pagination component
 * Shows page controls and items per page selector
 */
export function Pagination({
                               page,
                               totalPages,
                               totalItems,
                               pageSize,
                               onPageChange,
                               onPageSizeChange,
                               pageSizeOptions = [5, 10, 20, 50],
                               itemName = 'items',
                           }: PaginationProps) {
    const startIndex = totalItems === 0 ? 0 : (page - 1) * pageSize + 1;
    const endIndex = Math.min(page * pageSize, totalItems);

    return (
            <div className="mt-6 flex items-center justify-between border-t border-gray-200 pt-4">
                <div className="flex items-center gap-4">
                    <p className="text-sm text-gray-700">
                        Showing <span className="font-medium">{startIndex}</span> to{' '}
                        <span className="font-medium">{endIndex}</span> of{' '}
                        <span className="font-medium">{totalItems}</span> {itemName}
                    </p>
                    <select
                            value={pageSize}
                            onChange={(e) => onPageSizeChange(Number(e.target.value))}
                            className="rounded-md border-gray-300 py-1 pl-2 pr-8 text-sm focus:border-blue-500 focus:ring-blue-500"
                            aria-label="Items per page"
                    >
                        {pageSizeOptions.map((size) => (
                                <option key={size} value={size}>
                                    {size} / page
                                </option>
                        ))}
                    </select>
                </div>
                <div className="flex gap-2">
                    <button
                            onClick={() => onPageChange(page - 1)}
                            disabled={page === 1}
                            className={`rounded-md px-3 py-2 text-sm font-semibold ${
                                    page === 1
                                            ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                            : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-300'
                            }`}
                            aria-label="Previous page"
                    >
                        Previous
                    </button>
                    <button
                            onClick={() => onPageChange(page + 1)}
                            disabled={page === totalPages}
                            className={`rounded-md px-3 py-2 text-sm font-semibold ${
                                    page === totalPages
                                            ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                            : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-300'
                            }`}
                            aria-label="Next page"
                    >
                        Next
                    </button>
                </div>
            </div>
    );
}

export default Pagination;
