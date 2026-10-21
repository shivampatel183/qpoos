import { Outlet } from "react-router-dom";
import { Sidebar } from "./components/sidebar/sidebar";

export function Layout() {
    return (
        <Sidebar>
            <Outlet />
        </Sidebar>
    );
}