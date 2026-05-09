import { Card, Textarea, Button } from "@/components";

interface CaptionInputProps {
	caption: string;
	onChange: (val: string) => void;
}

export const CaptionInput = ({ caption, onChange }: CaptionInputProps) => (
	<Card className="flex flex-col gap-4 flex-9 h-full">
		<div className="font-semibold">Nhập nội dung bài viết</div>
		<Textarea value={caption} onChange={(e) => onChange(e.target.value)} placeholder="Nội dung bài viết..." className="flex-1"/>
		<div>
			<Button variant="outline" color="primary">Tạo caption từ AI</Button>
		</div>
	</Card>
);