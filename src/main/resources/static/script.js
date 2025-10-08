document.addEventListener("DOMContentLoaded", () => {
    const signUpButton = document.getElementById('signUp');
    const signInButton = document.getElementById('signIn');
    const container = document.getElementById('container');

    signUpButton.addEventListener('click', () => {
        container.classList.add("right-panel-active");
    });

    signInButton.addEventListener('click', () => {
        container.classList.remove("right-panel-active");
    });

    // Lógica para o formulário de Registro
    const signUpForm = document.getElementById("signUpForm");
    if (signUpForm) {
        signUpForm.addEventListener("submit", (e) => {
            e.preventDefault();
            const formData = new URLSearchParams(new FormData(signUpForm));

            fetch("/auth/cadastro", {
                method: "POST",
                body: formData,
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                },
            })
                .then((response) => response.text())
                .then((data) => {
                    alert(`Registro: ${data}`);
                    if (data === "Usuário registrado com sucesso!") {
                        container.classList.remove("right-panel-active");
                    }
                })
                .catch((error) => console.error("Erro no registro:", error));
        });
    }

    // Lógica para o formulário de Login
    const signInForm = document.getElementById("signInForm");
    if (signInForm) {
        signInForm.addEventListener("submit", (e) => {
            e.preventDefault();
            const formData = new URLSearchParams(new FormData(signInForm));

            fetch("/auth/login", {
                method: "POST",
                body: formData,
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                },
            })
                .then((response) => response.text())
                .then((data) => {
                    alert(`Login: ${data}`);
                })
                .catch((error) => console.error("Erro no login:", error));
        });
    }
});
