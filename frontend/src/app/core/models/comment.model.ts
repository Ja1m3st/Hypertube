// Importa al inicio del archivo:
import { FormsModule } from '@angular/forms';
import { CommentService } from '../services/comment.service';

export interface MovieComment {
	id: number;
	username: string;
	firstName: string;
	lastName: string;
	avatar?: string;
	text: string;
	createdAt: string;
}