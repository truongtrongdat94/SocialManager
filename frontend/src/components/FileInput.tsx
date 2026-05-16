import { type ChangeEvent, type InputHTMLAttributes, forwardRef } from "react";

interface FileInputProps extends Omit<
    InputHTMLAttributes<HTMLInputElement>,
    "type" | "onChange"
> {
    onFilesSelect: (files: File[]) => void;
}

const FileInput = forwardRef<HTMLInputElement, FileInputProps>(
    ({ onFilesSelect, accept = "image/*,video/*", multiple = false, ...props }, ref) => {
        const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
            const files = Array.from(e.target.files || []);
            onFilesSelect(files);

            if (e.target) {
                e.target.value = "";
            }
        };

        return (
            <input
                ref={ref}
                type="file"
                className="hidden"
                accept={accept}
                multiple={multiple}
                onChange={handleChange}
                {...props}
            />
        );
    },
);

FileInput.displayName = "FileInput";

export default FileInput;
