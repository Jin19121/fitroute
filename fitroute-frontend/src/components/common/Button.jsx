// src/components/common/Button.jsx

/**
 * Button
 * primary: blue fill
 * ghost: transparent with border
 * kakao: yellow KakaoTalk style
 */
const Button = ({
    children,
    variant = 'primary',
    isLoading = false,
    disabled = false,
    className = '',
    ...props
}) => {
    const base =
        'w-full rounded-[10px] py-[10px] text-[13px] font-semibold transition-all duration-150 flex items-center justify-center gap-2 select-none';

    const variants = {
        primary:
            'bg-[#4A7BFF] text-white hover:bg-[#3A6BEF] active:bg-[#2A5BDF] disabled:bg-[#EDEAE5] disabled:text-[#B8B4AE]',
        ghost:
            'bg-transparent border border-[#EDEAE5] text-[#6B6866] hover:border-[#4A7BFF] hover:text-[#4A7BFF]',
        kakao:
            'bg-[#FEE500] text-[#3C1E1E] hover:bg-[#EDD500] active:bg-[#DCC500]',
    };

    return (
        <button
            disabled={disabled || isLoading}
            className={[base, variants[variant], className].join(' ')}
            {...props}
        >
            {isLoading ? (
                <>
                    <span className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
                    <span>처리 중...</span>
                </>
            ) : (
                children
            )}
        </button>
    );
};

export default Button;
