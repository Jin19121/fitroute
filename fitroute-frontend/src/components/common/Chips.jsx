// src/components/common/OptionChip.jsx
export const OptionChip = ({ label, selected, onClick }) => (
    <button
        type="button"
        onClick={onClick}
        className={[
            'text-[11px] font-medium px-3 py-[5px] rounded-full border transition-all duration-150',
            selected
                ? 'bg-[#EEF3FF] border-[#4A7BFF] text-[#2A5CC5]'
                : 'bg-white border-[#EDEAE5] text-[#6B6866] hover:border-[#4A7BFF]',
        ].join(' ')}
    >
        {label}
    </button>
);

// src/components/common/DayChip.jsx
export const DayChip = ({ label, selected, onClick }) => (
    <button
        type="button"
        onClick={onClick}
        className={[
            'w-8 h-8 rounded-full text-[11px] font-semibold border transition-all duration-150 flex items-center justify-center',
            selected
                ? 'bg-[#4A7BFF] border-[#4A7BFF] text-white'
                : 'bg-white border-[#EDEAE5] text-[#B8B4AE] hover:border-[#4A7BFF]',
        ].join(' ')}
    >
        {label}
    </button>
);

// src/components/common/StepIndicator.jsx
export const StepIndicator = ({ total, current }) => (
    <div className="flex gap-1 justify-center mb-1">
        {Array.from({ length: total }).map((_, i) => {
            const isDone = i < current - 1;
            const isActive = i === current - 1;
            return (
                <div
                    key={i}
                    className={[
                        'h-[3px] rounded-full transition-all duration-300',
                        isActive
                            ? 'w-5 bg-[#4A7BFF]'
                            : isDone
                                ? 'w-3 bg-[#4A7BFF] opacity-40'
                                : 'w-3 bg-[#D5D0CA]',
                    ].join(' ')}
                />
            );
        })}
    </div>
);
