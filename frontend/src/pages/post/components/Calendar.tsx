import { DayPicker } from "react-day-picker";
import "react-day-picker/style.css";

interface CalendarProps {
	selected?: Date;
	onSelect: (date?: Date) => void;
}

export function Calendar({ selected, onSelect }: CalendarProps) {
	return (
		<DayPicker
			mode="single"
			selected={selected}
			onSelect={onSelect}
			fixedWeeks
			animate={true}
			navLayout="around"
			classNames={{
				caption_label: "text-sm font-semibold flex items-center justify-center",
				day: "text-xs p-0",
			}}
		/>
	);
}