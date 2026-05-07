import BottomNav from '../../components/common/BottomNav';

export default function WorkoutPage() {
    // ...
    return (
        <div className="flex flex-col h-full relative">  {/* relative 추가 */}
            {/* <TabBar tabs={TABS} active={tab} onChange={changeTab} />
            {tab === 'today' && <WorkoutTodayTab />}
            {tab === 'plan' && <WorkoutPlanTab />} */}
            <BottomNav />
        </div>
    );
}