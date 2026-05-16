import type {
    TextareaHTMLAttributes,
} from "react";

import { cn } from "@/utils";

interface TextareaProps
    extends TextareaHTMLAttributes<HTMLTextAreaElement> {
    className?: string;
}

const Textarea = ({
                      className,
                      ...props
                  }: TextareaProps) => {
    return (
        <div
            className={cn(
                "h-full w-full rounded-md",
                "ring-1 ring-border transition-all duration-200",
                "focus-within:ring-2 focus-within:ring-accent",
            )}
        >
            <textarea
                className={cn(
                    "h-full w-full resize-none bg-transparent p-2",
                    "text-left align-top outline-none",
                    className,
                )}
                {...props}
            />
        </div>
    );
};

export default Textarea;