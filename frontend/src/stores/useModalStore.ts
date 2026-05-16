import { create } from "zustand";
import type { ReactNode } from "react";

interface ModalState {
	isOpen: boolean;
	title: string;
	content: ReactNode;

	open: (
		title: string,
		content: ReactNode,
	) => void;

	close: () => void;
}

export const useModalStore =
	create<ModalState>((set) => ({
		isOpen: false,
		title: "",
		content: null,

		open: (title, content) =>
			set({
				isOpen: true,
				title,
				content,
			}),

		close: () =>
			set({
				isOpen: false,
			}),
	}));