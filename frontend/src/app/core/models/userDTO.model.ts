
export interface RegisterRequestDTO {
	firstName: string;
	lastName: string;
	email: string;
	username: string;
	password: string;
}

export interface RegisterResponseDTO {
	isRegistered: boolean;
}

export interface LoginRequestDTO {
	email: string;
	password: string;
}

export interface LoginResponseDTO {
	token: string;
}

export interface RecoverRequestDTO {
	email: string;
}

export interface RecoverResponseDTO {
	isChange: boolean;
	message: string;
}

export interface ResetPasswordRequestDTO {
	token: string;
	newPassword: string;
}

export interface ResetPasswordResponseDTO {
	message: string;
}


export interface ProfileUserResponseDTO {
	username: string;
	firstName: string;
	lastName: string;
	email: string;
	avatar: string;
	language?: string;
}

export interface ProfileUserRequestDTO {
	token: string;
}

