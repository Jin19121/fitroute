// src/components/common/Input.jsx
import { forwardRef } from 'react';

/**
 * Input
 * Matches the design: warm white bg, thin border, blue focus ring
 */
const Input = forwardRef(
    ({ label, error, hint, type = 'text', className = '', ...props }, ref) => {
        return (
            <div className="flex flex-col gap-1">
                {label && (
                    <label className="text-[11px] text-[#6B6866] font-medium">
                        {label}
                        {hint && (
                            <span className="text-[10px] text-[#B8B4AE] font-normal ml-1">
                                {hint}
                            </span>
                        )}
                    </label>
                )}
                <input
                    ref={ref}
                    type={type}
                    className={[
                        'w-full bg-white border rounded-[9px] px-3 py-2',
                        'text-[13px] text-[#1A1A1A] placeholder:text-[#B8B4AE]',
                        'outline-none transition-colors duration-150',
                        error
                            ? 'border-red-400 focus:border-red-400'
                            : 'border-[#EDEAE5] focus:border-[#4A7BFF]',
                        'disabled:opacity-50 disabled:cursor-not-allowed',
                        className,
                    ].join(' ')}
                    {...props}
                />
                {error && (
                    <p className="text-[10px] text-red-500 mt-0.5">{error}</p>
                )}
            </div>
        );
    },
);

Input.displayName = 'Input';
export default Input;
