# Reproductor de Música con Gestos de Proximidad
**Proyecto Integrador — Desarrollo de Aplicaciones Móviles**

> **Cuatrimestre:** 4-D  
> **Fecha de entrega:** 11 de diciembre de 2025

---

## Equipo de Desarrollo

| Nombre Completo | Rol / Tareas Principales | Usuario GitHub |
| :--- | :--- | :--- |
| Uziel Vasquez Martinez | Creador del repositorio. Desarrollo del sensor de proximidad y de la API REST. | [@uzielvasquezmartinez](https://github.com/uzielvasquezmartinez) |
| Gamaliel Leonel Martinez Villegas | Encargado del backend. | [@GamalielMartinez777](https://github.com/GamalielMartinez777) |
| Dulce Yoselin Pedraza Ocampo | Encargada de las pantallas y diseño UI/UX. | [@Dulce127](https://github.com/Dulce127) |

---

# Descripción del Proyecto

### ¿Qué hace la aplicación?
Es un reproductor de música para Android que combina canciones locales del dispositivo con canciones disponibles en un servidor de red. La aplicación permite a los usuarios gestionar sus propias playlists (crearlas, borrarlas y añadirles canciones).

Su característica principal es el **control por gestos**: el usuario puede pausar, reanudar, saltar o retroceder canciones sin tocar la pantalla, simplemente moviendo la mano sobre el sensor de proximidad del teléfono.

### Objetivo
Demostrar la implementación de una arquitectura robusta (**MVVM**) en Android, consumiendo una API REST propia con Retrofit para operaciones CRUD y utilizando el hardware del dispositivo (sensor de proximidad) para ofrecer una experiencia de usuario innovadora.

---

# Stack Tecnológico y Características

Este proyecto ha sido desarrollado siguiendo estrictamente los lineamientos de la materia:

- **Lenguaje:** Kotlin 100%.
- **Interfaz de Usuario:** Jetpack Compose.
- **Arquitectura:** MVVM (Model-View-ViewModel).
- **Conectividad (API REST):** Retrofit consumiendo un servidor local hecho en Python (Flask).
- **Métodos CRUD Implementados:**
  - **GET:** Obtiene la lista de canciones del servidor y la lista de playlists.
  - **POST:** Crea nuevas playlists y sube archivos `.mp3` de canciones locales al servidor para integrarlas.
  - **PUT:** Actualiza una playlist existente (por ejemplo, al añadirle el ID de una nueva canción).
  - **DELETE:** Elimina una playlist del servidor.
- **Sensor Integrado:** Sensor de Proximidad.
- **Uso del Sensor (Gestos Aéreos):**
  - **Hover (dejar la mano encima):** Pausa la música. Al retirar la mano, se reanuda.
  - **Wave (pasar la mano una vez):** Salta a la siguiente canción.
  - **Double Wave (dos pasadas):** Regresa a la canción anterior.

---

# Capturas de Pantalla

| Pantalla Principal | Gestión de Playlists | Uso del Sensor (UI) |
| :---: | :---: | :---: |
| Pantalla Principal | Playlists | [Servidor](https://github.com/user-attachments/assets/dbd094ba-f823-4839-a0da-3253799ac445) |

---

# Instalación y Releases

El ejecutable firmado (`.apk`) se encuentra disponible en la sección **Releases** de este repositorio.

1. Ve a la sección **Releases** del repositorio o [haz clic aquí](https://github.com/usuario/repositorio/releases).

2. Descarga el archivo **app-release.apk** de la última versión disponible.
3. Instálalo en tu dispositivo Android  
   *(recuerda habilitar la opción de “instalar desde orígenes desconocidos” si se te solicita)*.
