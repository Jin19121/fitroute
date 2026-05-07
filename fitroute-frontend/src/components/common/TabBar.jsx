// components/common/TabBar.jsx
export default function TabBar({ tabs, active, onChange }) {
    return (
        <div className="flex border-b border-gray-200 bg-white sticky top-0 z-10">
            {tabs.map(({ key, label }) => (
                <button
                    key={key}
                    onClick={() => onChange(key)}
                    className={`flex-1 py-3 text-sm font-medium transition-colors
            ${active === key
                            ? 'text-green-600 border-b-2 border-green-600'
                            : 'text-gray-400'
                        }`}
                >
                    {label}
                </button>
            ))}
        </div>
    );
}