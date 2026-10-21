// Shared domain types used across context, services, and components.
// Import from here — never redefine these in individual files.

export interface AuthUser {
    id: string;
    name: string;
    email: string;
    mobile?: string;
    role?: string;
    avatarUrl?: string;
}

export interface Company {
    id: string;
    name: string;
}
