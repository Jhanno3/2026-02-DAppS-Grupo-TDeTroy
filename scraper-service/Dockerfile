FROM mcr.microsoft.com/playwright:v1.64.1-jammy

WORKDIR /app
COPY package.json ./
RUN npm install

COPY server.js ./
EXPOSE 3001

CMD ["node", "server.js"]