import { Card } from "@/components";
import { Calendar } from "./Calendar.tsx";
import { TimePicker } from "./TimePicker.tsx";

interface SchedulePostProps {
	isDatePick: boolean;
	setIsDatePick: (val: boolean) => void;
	postDate?: Date;
	setPostDate: (val?: Date) => void;
	hour: number;
	setHour: (val: number) => void;
	minute: number;
	setMinute: (val: number) => void;
}

export const SchedulePost = ({ isDatePick, setIsDatePick, postDate, setPostDate, hour, setHour, minute, setMinute }: SchedulePostProps) => (
	<Card className="flex flex-col gap-4 min-h-fit flex-4 h-full">
		<div className="font-semibold">Thời gian đăng bài</div>

		<div className="flex gap-8">
			<label className="flex gap-2 cursor-pointer items-center">
				<input
					className="cursor-pointer"
					type="radio"
					checked={!isDatePick}
					onChange={() => setIsDatePick(false)}
				/>
				<span>Đăng ngay</span>
			</label>

			<label className="flex gap-2 cursor-pointer items-center">
				<input
					className="cursor-pointer"
					type="radio"
					checked={isDatePick}
					onChange={() => setIsDatePick(true)}
				/>
				<span>Lên lịch</span>
			</label>
		</div>

		{isDatePick && (
			<div className="flex justify-between flex-1">
				<Calendar selected={postDate} onSelect={setPostDate}/>

				<TimePicker
					hour={hour}
					minute={minute}
					onChange={(h, m) => {
						setHour(h);
						setMinute(m);
					}}
				/>
			</div>
		)}
	</Card>
);