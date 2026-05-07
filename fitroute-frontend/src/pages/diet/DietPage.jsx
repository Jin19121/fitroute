// pages/diet/DietPage.jsx
import { useSearchParams } from 'react-router-dom';
import TabBar from '../../components/common/TabBar';
import DietTodayTab from './DietTodayTab';
import DietPlanTab from './DietPlanTab';
import BottomNav from '../../components/common/BottomNav';

const TABS = [
    { key: 'today', label: '오늘' },
    { key: 'plan', label: '계획' },
];

export default function DietPage() {
    const [params, setParams] = useSearchParams();
    const tab = params.get('tab') ?? 'today';

    const changeTab = (key) => setParams({ tab: key });

    return (
        <div className="flex flex-col h-full relative">  {/* relative 추가 */}
            <TabBar tabs={TABS} active={tab} onChange={changeTab} />
            {tab === 'today' && <DietTodayTab />}
            {tab === 'plan' && <DietPlanTab />}
            <BottomNav />
        </div>
    );
}