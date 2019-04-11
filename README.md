# MoodleDownloader

Herramienta de escritorio para descargar cursos de Moodle en los que sólo tienes acceso de lectura.
  * Moodle URL: Indica la url completa del curso de Moodle
  * Usuario y Password: Por defecto intenta acceder al Moodle en modo invitado (usuario y password guest), pero se puede indicar un usuario y password propios de Moodle para acceder al curso a descargar.

Es necesario descargar la herramienta [wkhtmltopdf](https://wkhtmltopdf.org/downloads.html) y poner el ejecutable en la misma carpeta o en el PATH para que sea accesible.

Programado con Java, librería gráfica Swing y [jsoup](http://www.jsoup.com).
