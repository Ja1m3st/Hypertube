NAME = hypertube

all: up logs

all2:
	cd backend/mvn spring-boot:run
	cd ../frontend npm start 

up:
	docker compose up --build -d
	make logs

down:
	docker compose down

clean:
	docker compose down -v --rmi all --remove-orphans

fclean: clean
	docker system prune -af

re: fclean up

rb:
	docker compose up -d backend
	docker compose restart backend
	make logs

rf:
	docker compose up -d frontend
	docker compose restart frontend

logs:
	docker compose logs -f

.PHONY: all up down clean fclean re logs re-back re-front

