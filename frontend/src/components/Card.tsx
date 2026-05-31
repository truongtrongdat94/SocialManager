import { type HTMLAttributes, type ReactNode } from "react";
import { cn } from "@/utils";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
    children: ReactNode;
}

const Card = ({ children, className, ...props }: CardProps) => {
    return (
        <div
            className={cn(
                "rounded-lg border border-border-elevation bg-surface-primary p-4 shadow-card transition-all duration-200",
                className,
            )}
            {...props}
        >
            {children}
        </div>
    );
};

export default Card;
