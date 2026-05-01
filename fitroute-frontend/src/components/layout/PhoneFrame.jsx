// src/components/layout/PhoneFrame.jsx
const PhoneFrame = ({ children, dark = false }) => (
    <div className={['min-h-screen flex items-center justify-center', dark ? 'bg-[#111]' : 'bg-[#E4E1DC]'].join(' ')}>
        {/* Desktop: phone shell */}
        <div className="hidden sm:block w-[390px] bg-[#1A1A1A] rounded-[38px] p-[9px] shadow-2xl">
            <div
                className={['rounded-[30px] overflow-hidden flex flex-col', dark ? 'bg-[#1A1A1A]' : 'bg-[#F9F7F5]'].join(' ')}
                style={{ minHeight: 780 }}
            >
                {children}
            </div>
        </div>
        {/* Mobile: full screen */}
        <div
            className={['sm:hidden w-full flex flex-col', dark ? 'bg-[#1A1A1A]' : 'bg-[#F9F7F5]'].join(' ')}
            style={{ minHeight: '100dvh' }}
        >
            {children}
        </div>
    </div>
);

export default PhoneFrame;