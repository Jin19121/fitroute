// hooks/useCalendar.js
import { useState, useEffect } from 'react';
import { getCalendar } from '../api/diet';

export function useCalendar(type) {
    const now = new Date();
    const [year, setYear] = useState(now.getFullYear());
    const [month, setMonth] = useState(now.getMonth() + 1);
    const [calendarData, setCalendarData] = useState([]);

    useEffect(() => {
        getCalendar({ year, month, type }).then(setCalendarData);
    }, [year, month, type]);

    return { calendarData, year, month, setMonth };
}