import type { InputHTMLAttributes, ReactNode } from "react";
import { cn } from "@/utils";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
    iconPosition?: "left" | "right";
    iconButtonFunction?: () => any;
    children?: ReactNode;
    className?: string;
}

const Input = ({
    iconPosition,
    iconButtonFunction,
    className,
    children,
    ...props
}: InputProps) => {
    return (
        <div
            className={cn(
                "flex h-10 items-center justify-center gap-2 rounded-md bg-surface-primary px-2 transition-all duration-200",
                "ring-1 ring-border",
                "has-[input:focus]:ring-2 has-[input:focus]:ring-accent",
                className,
                children && iconPosition === "right" && "flex-row-reverse",
            )}
        >
            {children && (
                <button className="py-2 cursor-pointer" onClick={iconButtonFunction}>
                    {children}
                </button>
            )}
            <input
                autoComplete="off"
                className="flex-1 bg-transparent py-2 text-text-primary outline-none"
                {...props}
            />
        </div>
    );
};

export default Input;
