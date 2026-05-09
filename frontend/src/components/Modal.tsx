import { Fragment } from "react";
import { Transition, TransitionChild } from "@headlessui/react";
import { X } from "lucide-react";

import { useModalStore } from "@/stores/useModalStore";

const Modal = () => {
	const {
		isOpen,
		title,
		content,
		close,
	} = useModalStore();

	return (
		<Transition
			show={isOpen}
			as={Fragment}
		>
			<div className="fixed inset-0 z-50 flex items-center justify-center text-text-primary">
				{/* Overlay */}
				<TransitionChild
					as={Fragment}
					enter="transition-opacity duration-200"
					enterFrom="opacity-0"
					enterTo="opacity-100"
					leave="transition-opacity duration-200"
					leaveFrom="opacity-100"
					leaveTo="opacity-0"
				>
					<div
						className="absolute inset-0 bg-black/40"
						onClick={close}
					/>
				</TransitionChild>

				{/* Modal */}
				<TransitionChild
					as={Fragment}
					enter="transition-all duration-200"
					enterFrom="opacity-0 scale-95"
					enterTo="opacity-100 scale-100"
					leave="transition-all duration-200"
					leaveFrom="opacity-100 scale-100"
					leaveTo="opacity-0 scale-95"
				>
					<div className="relative z-10 w-fit max-w-4xl rounded-2xl bg-surface-primary px-4 py-4">
						{/* Header */}
						<div className="mb-4 flex items-center justify-between">
							<div className="text-lg font-semibold">
								{title}
							</div>

							<button onClick={close} className="relative cursor-pointer">
								<div className="absolute -inset-2" />
								<X strokeWidth={1.5} size={16}/>
							</button>
						</div>

						{content}
					</div>
				</TransitionChild>
			</div>
		</Transition>
	);
};

export default Modal;