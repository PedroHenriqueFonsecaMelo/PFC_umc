# Pfc_umc

PFC UMC 2025

## Hierarquia de Pastas

### Como está atualmente

```
Pfc_umc/
│
├── src/
│   └── main/
│       └── java/
│           └── umc/
│               └── cfc/
│                   └── ProjetoCFC_Application.java
│
├── README.md
```

---

### Como deve ficar

```
Pfc_umc/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── umc/
│       │       └── cfc/
│       │           ├── ProjetoCFC_Application.java
│       │           ├── controller/      # Controllers REST
│       │           ├── service/         # Lógica de negócio
│       │           ├── repository/      # DAOs (acesso ao banco)
│       │           ├── model/           # Entidades JPA
│       │           ├── dto/             # DTOs (Data Transfer Objects)
│       │
│       └── resources/
│           ├── static/
│           │   ├── css/                 # Arquivos CSS
│           │   ├── js/                  # Arquivos JavaScript
│           │   └── img/                 # Imagens (opcional)
│           └── templates/               # Páginas HTML (Thymeleaf ou outros)
│           └── application.properties
│
├── users.db                             # Banco de dados SQLite
├── README.md
```

**Observações:**

- DAOs (repositórios) ficam em `src/main/java/umc/cfc/repository/`
- DTOs ficam em `src/main/java/umc/cfc/dto/`
- HTMLs ficam em `src/main/resources/templates/`
- CSS e JS ficam em `src/main/resources/static/`
