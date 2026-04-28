FROM node:20-alpine

WORKDIR /app
COPY package*.json ./

RUN npm install
RUN npm install -g @angular/cli 

COPY . .

EXPOSE 4000

CMD ["ng", "serve", "--host", "0.0.0.0", "--poll", "2000", "--proxy-config", "proxy.conf.json"]