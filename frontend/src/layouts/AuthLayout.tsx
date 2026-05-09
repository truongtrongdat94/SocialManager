import {Outlet} from "react-router";

export const AuthLayout = () => {
    return (
        <div className="flex h-screen w-full text-text-primary bg-surface-secondary">
            <div className="flex-1 flex justify-center items-center font-bold text-4xl">
                SocialManager
            </div>
            <div className="flex flex-1 flex-col bg-surface-primary">
                <Outlet />
            </div>
        </div>
    )
}