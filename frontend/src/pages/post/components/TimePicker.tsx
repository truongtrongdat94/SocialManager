import { ChevronUp, ChevronDown } from "lucide-react";
import { useEffect, useState } from "react";

interface TimePickerProps {
	hour?: number;
	minute?: number;
	onChange?: (hour: number, minute: number) => void;
}

export const TimePicker = ({ hour = 0, minute = 0, onChange }: TimePickerProps) => {
	const [hours, setHours] = useState(hour.toString().padStart(2, "0"));
	const [minutes, setMinutes] = useState(minute.toString().padStart(2, "0"));

	const clampHours = (value: number) => {
		if (value < 0) return 23;
		if (value > 23) return 0;
		return value;
	};

	const clampMinutes = (value: number) => {
		if (value < 0) return 59;
		if (value > 59) return 0;
		return value;
	};

	const updateHours = (value: number) => {
		const normalized = clampHours(value);
		const formatted = normalized.toString().padStart(2, "0");
		setHours(formatted);
		onChange?.(normalized, Number(minutes));
	};

	const updateMinutes = (value: number) => {
		const normalized = clampMinutes(value);
		const formatted = normalized.toString().padStart(2, "0");
		setMinutes(formatted);
		onChange?.(Number(hours), normalized);
	};

	const handleHourInput = (value: string) => {
		setHours(value.replace(/\D/g, ""));
	};

	const handleMinuteInput = (value: string) => {
		setMinutes(value.replace(/\D/g, ""));
	};

	const validateHours = () => {
		const parsed = Number(hours);
		if (isNaN(parsed) || parsed < 0) {
			updateHours(0);
			return;
		}
		updateHours(parsed);
	};

	const validateMinutes = () => {
		const parsed = Number(minutes);
		if (isNaN(parsed) || parsed < 0) {
			updateMinutes(0);
			return;
		}
		updateMinutes(parsed);
	};

	useEffect(() => {
		onChange?.(Number(hours), Number(minutes));
	}, []);

	return (
		<div className="flex items-center gap-3">
			{/* Hours */}
			<div className="flex flex-col items-center gap-2">
				<button
					type="button"
					onClick={() => updateHours(Number(hours) + 1)}
					className="cursor-pointer rounded-md p-1 transition-colors hover:bg-surface-secondary"
				>
					<ChevronUp size={18} strokeWidth={1.5} />
				</button>

				<input
					value={hours}
					onChange={(e) => handleHourInput(e.target.value)}
					onBlur={validateHours}
					maxLength={2}
					className="h-14 w-16 rounded-xl ring-1 ring-border bg-surface-primary text-center text-2xl font-semibold outline-none transition-all focus:ring-2 focus:ring-accent"
				/>

				<button
					type="button"
					onClick={() => updateHours(Number(hours) - 1)}
					className="cursor-pointer rounded-md p-1 transition-colors hover:bg-surface-secondary"
				>
					<ChevronDown size={18} strokeWidth={1.5} />
				</button>
			</div>

			<div className="pt-1 text-3xl font-semibold">:</div>

			<div className="flex flex-col items-center gap-2">
				<button
					type="button"
					onClick={() => updateMinutes(Number(minutes) + 1)}
					className="cursor-pointer rounded-md p-1 transition-colors hover:bg-surface-secondary"
				>
					<ChevronUp size={18} strokeWidth={1.5} />
				</button>

				<input
					value={minutes}
					onChange={(e) => handleMinuteInput(e.target.value)}
					onBlur={validateMinutes}
					maxLength={2}
					className="h-14 w-16 rounded-xl ring-1 ring-border bg-surface-primary text-center text-2xl font-semibold outline-none transition-all focus:ring-2 focus:ring-accent"
				/>

				<button
					type="button"
					onClick={() => updateMinutes(Number(minutes) - 1)}
					className="cursor-pointer rounded-md p-1 transition-colors hover:bg-surface-secondary"
				>
					<ChevronDown size={18} strokeWidth={1.5} />
				</button>
			</div>
		</div>
	);
};