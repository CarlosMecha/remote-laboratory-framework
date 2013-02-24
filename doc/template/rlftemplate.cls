\ProvidesClass{rlftemplate}[2011/09/01 Clase de documentos del proyecto RLF.] 

\NeedsTeXFormat{LaTeX2e}
\LoadClass[a4paper,titlepage]{book}

% Paquetes
\usepackage[utf8]{inputenc}
\usepackage[spanish,activeacute]{babel}
\usepackage[table]{xcolor}
\usepackage{longtable}
\usepackage{fancyhdr}
\usepackage{verbatim}
\usepackage{graphicx}
\usepackage{fancyhdr}
\usepackage{hyperref}
\usepackage{makeidx}
\usepackage{lib/multirow}
\usepackage{lib/eurosym}
\usepackage{lib/titlesec}
\usepackage{lib/here}
\usepackage{lib/rlf}


% Márgenes
\usepackage[top=2.6cm, bottom=2.8cm, left=2cm, right=2cm]{geometry}
\headsep=30pt
\footskip=32pt
\oddsidemargin=20pt
\evensidemargin=10pt
\textwidth=15cm
\textheight=23.5cm

% Otras opciones
\definecolor{gray90}{gray}{0.9}
\definecolor{gray60}{gray}{0.6}
\definecolor{gray30}{gray}{0.3}
\clubpenalty=10000
\widowpenalty=10000
\pagestyle{fancy}
\makeindex

\parindent 2em
\parskip 3ex

% Cabecera
\lhead[\textcolor{gray60}{\RLF}]{}
\rhead[]{\textcolor{gray60}{\fancyplain{}{\textit{\leftmark}}}}

% Pie de página
\lfoot[\thepage]{}
\cfoot[]{}
\rfoot[]{\thepage}
