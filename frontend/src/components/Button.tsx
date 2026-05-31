import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/utils";

type ButtonVariant = "solid" | "soft" | "outline";
type ButtonColor = "default" | "primary" | "danger" | "info" | "success";
type ButtonSize = "sm" | "md" | "lg";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: ButtonVariant;
    color?: ButtonColor;
    size?: ButtonSize;
    disableHover?: boolean;
    children?: ReactNode;
    className?: string;
}

const baseClasses =
    "flex justify-center items-center font-medium transition-all duration-200 cursor-pointer";

const colorVariantClasses: Record<
    ButtonColor,
    Record<ButtonVariant, string>
> = {
    default: {
        solid: "",
        soft: "bg-button-soft-default-bg hover:bg-button-soft-default-bg-hover text-button-soft-default-text",
        outline:
            "bg-button-outline-default-bg hover:bg-button-outline-default-bg-hover text-button-outline-default-text border" +
            " border-button-outline-default-border",
    },
    primary: {
        solid: "bg-button-solid-primary-bg hover:bg-button-solid-primary-bg-hover text-button-solid-primary-text",
        soft: "bg-button-soft-primary-bg hover:bg-button-soft-primary-bg-hover text-button-soft-primary-text",
        outline:
            "bg-button-outline-primary-bg hover:bg-button-outline-primary-bg-hover text-button-outline-primary-text border border-button-outline-primary-border",
    },

    danger: {
        solid: "bg-button-solid-danger-bg hover:bg-button-solid-danger-bg-hover text-button-solid-danger-text",
        soft: "bg-button-soft-danger-bg hover:bg-button-soft-danger-bg-hover text-button-soft-danger-text",
        outline:
            "bg-button-outline-danger-bg hover:bg-button-outline-danger-bg-hover text-button-outline-danger-text border border-button-outline-danger-border",
    },

    info: {
        solid: "bg-button-solid-info-bg hover:bg-button-solid-info-bg-hover text-button-solid-info-text",
        soft: "bg-button-soft-info-bg hover:bg-button-soft-info-bg-hover text-button-soft-info-text",
        outline:
            "bg-button-outline-info-bg hover:bg-button-outline-info-bg-hover text-button-outline-info-text border border-button-outline-info-border",
    },

    success: {
        solid: "bg-button-solid-success-bg hover:bg-button-solid-success-bg-hover text-button-solid-success-text",
        soft: "bg-button-soft-success-bg hover:bg-button-soft-success-bg-hover text-button-soft-success-text",
        outline:
            "bg-button-outline-success-bg hover:bg-button-outline-success-bg-hover text-button-outline-success-text border border-button-outline-success-border",
    },
};

const hoverDisabledClasses: Record<
    ButtonColor,
    Record<ButtonVariant, string>
> = {
    default: {
        solid: "",
        soft: "hover:bg-button-soft-default-bg",
        outline: "hover:bg-button-outline-default-bg",
    },
    primary: {
        solid: "hover:bg-button-solid-primary-bg",
        soft: "hover:bg-button-soft-primary-bg",
        outline: "hover:bg-button-outline-primary-bg",
    },
    danger: {
        solid: "hover:bg-button-solid-danger-bg",
        soft: "hover:bg-button-soft-danger-bg",
        outline: "hover:bg-button-outline-danger-bg",
    },
    info: {
        solid: "hover:bg-button-solid-info-bg",
        soft: "hover:bg-button-soft-info-bg",
        outline: "hover:bg-button-outline-info-bg",
    },
    success: {
        solid: "hover:bg-button-solid-success-bg",
        soft: "hover:bg-button-soft-success-bg",
        outline: "hover:bg-button-outline-success-bg",
    },
};

const sizeClasses = {
    sm: "px-3 h-8 text-xs font-medium rounded-sm",
    md: "px-4 h-10 text-sm font-medium rounded-md",
    lg: "px-5 h-12 text-base font-medium rounded-lg",
};

const Button = ({
    variant = "solid",
    color = "default",
    size = "md",
    disableHover = false,
    children,
    className,
    ...props
}: ButtonProps) => {
    return (
        <button
            {...props}
            className={cn(
                baseClasses,
                colorVariantClasses[color][variant],
                sizeClasses[size],
                disableHover && hoverDisabledClasses[color][variant],
                className,
            )}
        >
            {children}
        </button>
    );
};

export default Button;
