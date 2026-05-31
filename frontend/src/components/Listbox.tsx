import {Fragment} from "react";
import {cn} from "@/utils";
import {Listbox as HeadlessListbox, ListboxButton, ListboxOption, ListboxOptions, Transition,} from "@headlessui/react";
import {Check, ChevronDownIcon} from "lucide-react";

interface ListboxOption {
    label: string;
    value: string | number | undefined;
}

interface SelectProps {
    name?: string;
    selected?: ListboxOption;
    options?: ListboxOption[];
    disabled?: boolean;
    width?: string;
    height?: string;
    onChange?: (value: string | number) => void;
    selectedContainerClassname?: string | ((open: boolean) => string);
    selectedItemClassname?: string | ((open: boolean) => string);
    optionClassname?: string | ((open: boolean) => string);
    optionsClassname?: string | ((open: boolean) => string);
}

const Listbox = ({
    name,
    selected,
    options,
    disabled = false,
    width,
    height,
    onChange,
    selectedContainerClassname,
    selectedItemClassname,
    optionClassname,
    optionsClassname,
}: SelectProps) => {

    return (
        <HeadlessListbox
            name={name}
            value={selected?.value}
            disabled={disabled}
            onChange={(value) => onChange?.(value)}
        >
            {({ open }) => (
                <div className={cn("relative", width, height)}>
                    <div
                        className={cn(
                            "flex w-full rounded-md transition-all duration-200",
                            "ring-1 ring-border",
                            typeof selectedContainerClassname === "function"
                                ? selectedContainerClassname(open)
                                : selectedContainerClassname,
                            open && "ring-2 ring-accent",
                        )}
                    >
                        <ListboxButton
                            className={cn(
                                "flex w-full cursor-pointer items-center justify-between gap-2 p-2 ring-none outline-none",
                                selectedItemClassname,
                            )}
                        >
                            <span className="truncate">{selected?.label}</span>
                            <ChevronDownIcon size={20} strokeWidth={1.5} />
                        </ListboxButton>
                    </div>
                    <Transition
                        show={open}
                        as={Fragment}
                        enter="transition ease-out duration-200"
                        enterFrom="opacity-0 scale-95"
                        enterTo="opacity-100 scale-100"
                        leave="transition ease-in duration-200"
                        leaveFrom="opacity-100 scale-100"
                        leaveTo="opacity-0 scale-95"
                    >
                        <ListboxOptions
                            portal={true}
                            anchor={{ to: "bottom", gap: "8px" }}
                            className={cn(
                                "w-(--button-width) shadow-card z-10 overflow-auto rounded-2xl border border-border bg-surface-primary",
                                "ring-none outline-none text-text-primary",
                                optionsClassname,
                            )}
                        >
                            {options?.map((option, index) => (
                                <ListboxOption
                                    key={index}
                                    value={option.value}
                                    className={cn(
                                        "h-10 rounded-lg flex items-center justify-between outline-none cursor-pointer truncate overflow-hidden m-2 px-2",
                                        "data-focus:bg-question-table-hover",
                                        optionClassname,
                                    )}
                                >
                                    <span
                                        className={cn(
                                            option.value == selected?.value &&
                                                "font-medium text-accent",
                                        )}
                                    >
                                        {option.label}
                                    </span>
                                    {option.value == selected?.value && (
                                        <Check
                                            size={18}
                                            strokeWidth={1.5}
                                            className="text-accent"
                                        />
                                    )}
                                </ListboxOption>
                            ))}
                        </ListboxOptions>
                    </Transition>
                </div>
            )}
        </HeadlessListbox>
    );
};

export default Listbox;
