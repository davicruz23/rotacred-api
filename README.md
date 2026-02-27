# 🛠️ API Gestão – Spring Boot

API REST desenvolvida em **Java com Spring Boot**, responsável pelo backend do sistema de gestão RotaCred.  
A aplicação foi construída com foco em organização arquitetural, segurança, boas práticas REST e escalabilidade.

Esta API fornece endpoints para gerenciamento de entidades do sistema, autenticação e controle de acesso, além de integração com aplicações web e mobile.

---

## 🚀 Tecnologias Utilizadas

- **Java**
- **Spring Boot**
- **Spring Security**
- **JWT / OAuth2**
- **Spring Data JPA / Hibernate**
- **MySQL / PostgreSQL**
- **Maven**
- **Docker / Docker Compose**
- **Git**

---

## 🧱 Arquitetura do Projeto

O projeto segue o padrão de **arquitetura em camadas**, promovendo separação de responsabilidades:

- **Controller** → Responsável pelos endpoints REST
- **Service** → Regras de negócio
- **Repository** → Acesso a dados via JPA
- **DTOs** → Transferência segura de dados
- **Config** → Configurações de segurança e aplicação
- **Exception Handler** → Tratamento global de erros

Essa organização facilita manutenção, testes e evolução do sistema.

---

## 🔐 Segurança

A API implementa controle de acesso utilizando:

- Autenticação baseada em **JWT**
- Configuração de segurança com **Spring Security**
- Proteção de rotas por perfil/permissão

As rotas protegidas exigem token válido no header:

```
Authorization: Bearer {token}
```

---

## 📌 Funcionalidades Principais

✔️ CRUD completo de entidades do sistema  
✔️ Autenticação e autorização de usuários  
✔️ Validação de dados com Bean Validation  
✔️ Tratamento global de exceções  
✔️ Respostas HTTP padronizadas  
✔️ Integração com frontend e aplicação mobile  
✔️ Estrutura preparada para expansão futura  

---

## 🛠️ Como Rodar Localmente

### 🔹 Pré-requisitos

- Java 17+  
- Maven  
- Banco de dados configurado (MySQL ou PostgreSQL)  
- Docker (opcional)

---

### ▶️ Executando com Maven

```bash
git clone https://github.com/davicruz23/api-gestao.git
cd api-gestao

mvn clean install
mvn spring-boot:run
```

A aplicação ficará disponível em:

```
http://localhost:8080
```

---

### 🐳 Executando com Docker

```bash
docker compose up --build
```

Isso irá subir a aplicação junto com o banco de dados configurado no `docker-compose.yml`.

---

## ⚙️ Configuração

As configurações principais estão no arquivo:

```
src/main/resources/application.properties
```

Exemplo de configuração:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/rotacred
spring.datasource.username=root
spring.datasource.password=senha

jwt.secret=chaveSecreta
jwt.expiration=86400000
```

---

## 📊 Padrões REST Utilizados

- **GET** → Listagem e consulta
- **POST** → Criação
- **PUT** → Atualização
- **DELETE** → Remoção
- Uso de códigos HTTP adequados (200, 201, 400, 401, 404, 500)

---

## 🧠 Boas Práticas Aplicadas

- Separação clara de responsabilidades
- Uso de DTOs para evitar exposição direta das entidades
- Tratamento centralizado de exceções
- Estrutura preparada para escalabilidade
- Código organizado seguindo princípios SOLID

---

## 🔗 Integração

Esta API é consumida por:

- Aplicação Web (React)
- Aplicação Mobile (Flutter)

---

## 👨‍💻 Autor

**Davi Cruz**  
Desenvolvedor Back-End Java  

GitHub: https://github.com/davicruz23  
Email: davifieledeus@gmail.com
