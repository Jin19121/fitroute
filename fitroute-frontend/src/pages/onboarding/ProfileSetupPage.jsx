// pages/onboarding/ProfileSetupPage.jsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Button from "../../components/common/Button";
import Input from "../../components/common/Input";
import OptionChip from "../../components/common/OptionChip";
import useOnboardingStore from "../../store/onboardingStore";

export default function ProfileSetupPage() {
    const navigate = useNavigate();
    const { setProfile } = useOnboardingStore();

    const [gender, setGender] = useState(null);       // "MALE" | "FEMALE"
    const [birthDate, setBirthDate] = useState("");   // "YYYY-MM-DD"
    const [height, setHeight] = useState("");
    const [weight, setWeight] = useState("");

    const isValid = gender && birthDate && height && weight;

    const handleNext = () => {
        setProfile({ gender, birthDate, height: parseFloat(height), weight: parseFloat(weight) });
        navigate("/onboarding/ai-setup");
    };

    return (
        <div className="flex flex-col gap-6 p-6">
            <h2 className="text-xl font-bold">기본 정보를 알려주세요</h2>

            {/* 성별 - 칩 선택 */}
            <div>
                <p className="text-sm text-gray-500 mb-2">성별</p>
                <div className="flex gap-3">
                    <OptionChip
                        label="남성"
                        selected={gender === "MALE"}
                        onClick={() => setGender("MALE")}
                    />
                    <OptionChip
                        label="여성"
                        selected={gender === "FEMALE"}
                        onClick={() => setGender("FEMALE")}
                    />
                </div>
            </div>

            {/* 생년월일 */}
            <Input
                label="생년월일"
                type="date"
                value={birthDate}
                onChange={(e) => setBirthDate(e.target.value)}
                max={new Date().toISOString().split("T")[0]}
            />

            {/* 키/체중 */}
            <Input
                label="키 (cm)"
                type="number"
                value={height}
                onChange={(e) => setHeight(e.target.value)}
                placeholder="예) 175"
            />
            <Input
                label="현재 체중 (kg)"
                type="number"
                value={weight}
                onChange={(e) => setWeight(e.target.value)}
                placeholder="예) 75"
            />

            <Button disabled={!isValid} onClick={handleNext}>
                다음
            </Button>
        </div>
    );
}