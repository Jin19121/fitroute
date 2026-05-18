// src/components/report/WeightChart.jsx

const CHART_W = 320;
const CHART_H = 120;
const PAD = { top: 16, right: 12, bottom: 28, left: 36 };

function buildPath(points, minW, maxW, minX, maxX) {
    if (points.length < 2) return '';

    const xRange = maxX - minX || 1;
    const wRange = maxW - minW || 1;

    const toX = (ts) =>
        PAD.left + ((ts - minX) / xRange) * (CHART_W - PAD.left - PAD.right);
    const toY = (w) =>
        PAD.top + (1 - (w - minW) / wRange) * (CHART_H - PAD.top - PAD.bottom);

    return points
        .map((p, i) => `${i === 0 ? 'M' : 'L'} ${toX(p.ts).toFixed(1)} ${toY(p.w).toFixed(1)}`)
        .join(' ');
}

export default function WeightChart({ measurements }) {
    if (!measurements || measurements.length === 0) {
        return (
            <div className="bg-white rounded-2xl p-4 flex items-center justify-center h-32">
                <span className="text-[12px] text-[#b8b4ae]">이번 달 체중 기록이 없어요</span>
            </div>
        );
    }

    // 날짜를 timestamp로 변환
    const points = measurements.map((m) => ({
        ts: new Date(m.date).getTime(),
        w: m.weightKg,
        label: m.date.slice(8), // DD
    }));

    const weights = points.map((p) => p.w);
    const timestamps = points.map((p) => p.ts);

    const minW = Math.min(...weights);
    const maxW = Math.max(...weights);
    const minX = Math.min(...timestamps);
    const maxX = Math.max(...timestamps);

    // Y축 눈금 (소수점 1자리)
    const wRange = maxW - minW || 1;
    const yTicks = [minW, minW + wRange / 2, maxW].map((v) =>
        parseFloat(v.toFixed(1))
    );

    const xRange = maxX - minX || 1;
    const toX = (ts) =>
        PAD.left + ((ts - minX) / xRange) * (CHART_W - PAD.left - PAD.right);
    const toY = (w) =>
        PAD.top + (1 - (w - minW) / wRange) * (CHART_H - PAD.top - PAD.bottom);

    const linePath = buildPath(points, minW, maxW, minX, maxX);

    // 영역 채우기 경로
    const firstX = toX(points[0].ts).toFixed(1);
    const lastX = toX(points[points.length - 1].ts).toFixed(1);
    const bottom = (CHART_H - PAD.bottom).toFixed(1);
    const areaPath = `${linePath} L ${lastX} ${bottom} L ${firstX} ${bottom} Z`;

    return (
        <div className="bg-white rounded-2xl p-4 flex flex-col gap-2">
            <span className="text-[11px] font-bold text-[#1a1a1a]">📈 체중 추이</span>

            <svg
                viewBox={`0 0 ${CHART_W} ${CHART_H}`}
                className="w-full"
                style={{ height: CHART_H }}
            >
                {/* Y축 눈금 라인 */}
                {yTicks.map((tick) => (
                    <g key={tick}>
                        <line
                            x1={PAD.left}
                            y1={toY(tick)}
                            x2={CHART_W - PAD.right}
                            y2={toY(tick)}
                            stroke="#f0ece5"
                            strokeWidth="1"
                        />
                        <text
                            x={PAD.left - 4}
                            y={toY(tick) + 3}
                            textAnchor="end"
                            fontSize="8"
                            fill="#b8b4ae"
                        >
                            {tick}
                        </text>
                    </g>
                ))}

                {/* 영역 채우기 */}
                <path d={areaPath} fill="#ff8c42" fillOpacity="0.08" />

                {/* 라인 */}
                <path
                    d={linePath}
                    fill="none"
                    stroke="#ff8c42"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                />

                {/* 데이터 포인트 + X축 라벨 */}
                {points.map((p) => (
                    <g key={p.ts}>
                        <circle
                            cx={toX(p.ts)}
                            cy={toY(p.w)}
                            r="3"
                            fill="#ff8c42"
                        />
                        <text
                            x={toX(p.ts)}
                            y={CHART_H - PAD.bottom + 12}
                            textAnchor="middle"
                            fontSize="8"
                            fill="#b8b4ae"
                        >
                            {p.label}일
                        </text>
                    </g>
                ))}
            </svg>

            {/* 최저 / 최고 요약 */}
            <div className="flex justify-between text-[10px] text-[#8a8680]">
                <span>최저 <b className="text-[#4a7bff]">{minW.toFixed(1)}kg</b></span>
                <span>최고 <b className="text-red-400">{maxW.toFixed(1)}kg</b></span>
            </div>
        </div>
    );
}