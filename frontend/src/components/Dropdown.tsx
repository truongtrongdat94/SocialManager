import {Fragment, type ReactNode} from "react";
import {cn} from "@/utils";
import {Menu as HeadlessMenu, MenuButton, MenuItem, MenuItems, Transition,} from "@headlessui/react";

export type DropdownVariant =
    | "default"
    | "primary"
    | "info"
    | "success"
    | "danger";

export interface DropdownAction {
    label: string;
    icon?: ReactNode;
    onClick?: () => void;
    variant?: DropdownVariant;
}

export interface DropdownZone {
    actions: DropdownAction[];
}

interface DropdownProps {
    trigger: ReactNode;
    zones: DropdownZone[];
    width?: string;
    position?: "top" | "bottom";
    menuClassname?: string;
}

const variantStyles: Record<DropdownVariant, string> = {
    default:
        "text-button-outline-default-text data-focus:bg-button-outline-default-bg-hover",
    primary:
        "text-button-outline-primary-text data-focus:bg-button-outline-primary-bg-hover",
    info: "text-button-outline-info-text data-focus:bg-button-outline-info-bg-hover",
    success:
        "text-button-outline-success-text data-focus:bg-button-outline-success-bg-hover",
    danger: "text-button-outline-danger-text data-focus:bg-button-outline-danger-bg-hover",
};

const Dropdown = ({
    trigger,
    zones,
    width = "w-64",
    position = "bottom",
    menuClassname,
}: DropdownProps) => {
	const positionClasses =
		position === "top"
			? "bottom-full mb-2 origin-bottom-right"
			: "top-full mt-2 origin-top-right";

    return (
	    <HeadlessMenu as="div" className="relative w-full text-left z-20">
		    <MenuButton
			    as="div"
			    className="w-full cursor-pointer outline-none"
		    >
                {trigger}
            </MenuButton>

            <Transition
                as={Fragment}
                enter="transition ease-out duration-200"
                enterFrom="opacity-0 scale-95"
                enterTo="opacity-100 scale-100"
                leave="transition ease-in duration-200"
                leaveFrom="opacity-100 scale-100"
                leaveTo="opacity-0 scale-95"
            >
                <MenuItems
                    className={cn(
                        "absolute right-0 shadow-card mt-2 origin-top-right overflow-auto rounded-2xl border border-border bg-surface-primary",
                        "ring-none outline-none",
	                    positionClasses,
                        width,
                        menuClassname,
                    )}
                >
                    {zones.map((zone, zoneIndex) => (
                        <Fragment key={zoneIndex}>
                            <div className="p-2 flex flex-col gap-1">
                                {zone.actions.map((action, actionIndex) => {
                                    const variant = action.variant || "default";

                                    return (
                                        <MenuItem key={actionIndex}>
                                            <button
                                                onClick={action.onClick}
                                                className={cn(
                                                    "w-full h-10 rounded-lg flex items-center gap-3 outline-none cursor-pointer truncate overflow-hidden px-2 transition-colors",
													"text-sm",
                                                    variantStyles[variant],
                                                )}
                                            >
                                                {action.icon && (
                                                    <span className="shrink-0">
                                                        {action.icon}
                                                    </span>
                                                )}
                                                <span className="font-medium truncate">
                                                    {action.label}
                                                </span>
                                            </button>
                                        </MenuItem>
                                    );
                                })}
                            </div>

                            {zoneIndex < zones.length - 1 && (
                                <div className="h-px bg-border mx-3" />
                            )}
                        </Fragment>
                    ))}
                </MenuItems>
            </Transition>
        </HeadlessMenu>
    );
};

export default Dropdown;
