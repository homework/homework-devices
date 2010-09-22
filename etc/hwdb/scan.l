%{
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "y.tab.h"
#include "mem.h"

extern YYSTYPE yylval;
%}

%%
SELECT			{ return SELECT; }
select			{ return SELECT; }
FROM			{ return FROM; }
from			{ return FROM; }
WHERE			{ return WHERE; }
where			{ return WHERE; }
CREATE			{ return CREATE; }
create			{ return CREATE; }
TABLE			{ return TABLETK; }
table			{ return TABLETK; }
INSERT			{ return INSERT; }
insert			{ return INSERT; }
INTO			{ return INTO;}
into			{ return INTO;}
VALUES			{ return VALUES;}
values			{ return VALUES;}

SHOW			{ return SHOW;}
show			{ return SHOW;}
TABLES			{ return TABLES;}
tables			{ return TABLES;}

ROWS			{ return ROWS;}
rows			{ return ROWS;}
RANGE			{ return RANGE;}
range			{ return RANGE;}
NOW			{ return NOW;}
now			{ return NOW;}
SECONDS			{ return SECONDS;}
seconds			{ return SECONDS;}
MINUTES			{ return MINUTES;}
minutes			{ return MINUTES;}
HOURS			{ return HOURS;}
hours			{ return HOURS;}
LAST			{ return LAST;}
last			{ return LAST;}
MILLIS			{ return MILLIS;}
millis			{ return MILLIS;}


boolean			{ return BOOLEAN;}
integer			{ return INTEGER;}
real			{ return REAL;}
character		{ return CHARACTER;}
varchar			{ return VARCHAR;}
blob			{ return BLOB;}
tinyint			{ return TINYINT;}
smallint		{ return SMALLINT;}
tstamp			{ return TSTAMP;}

true			{ return TRUETK;}
false			{ return FALSETK;}

and			{ return AND; }
AND			{ return AND; }
or			{ return OR; }
OR			{ return OR; }

save			{ return SAVE; }
SAVE			{ return SAVE; }
delete			{ return DELETE; }
DELETE			{ return DELETE; }
QUERY			{ return QUERY; }
query			{ return QUERY; }

as			{ return AS; }
AS			{ return AS; }

exec			{ return EXEC; }
EXEC			{ return EXEC; }

count			{ return COUNT; }
COUNT			{ return COUNT; }
min			{ return MIN; }
MIN			{ return MIN; }
max			{ return MAX; }
MAX			{ return MAX; }
avg			{ return AVG; }
AVG			{ return AVG; }
sum			{ return SUM; }
SUM			{ return SUM; }

order			{ return ORDER; }
ORDER			{ return ORDER; }
by			{ return BY; }
BY			{ return BY; }

subscribe		{ return SUBSCRIBE;}
SUBSCRIBE		{ return SUBSCRIBE;}
publish			{ return PUBLISH;}
PUBLISH			{ return PUBLISH;}
unsubscribe		{ return UNSUBSCRIBE; }
UNSUBSCRIBE		{ return UNSUBSCRIBE; }

[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+ { yylval.string = str_dupl(yytext); return IPADDR; }

%[0-9]+\.[0-9]+		{ yylval.numfloat = atof(yytext); return NUMFLOAT; }
%[0-9]+			{ yylval.number = atoi(yytext); return NUMBER; }

[0-9]+\.[0-9]+		{ yylval.string = str_dupl(yytext); return NUMFLOAT; }
\-[0-9]+\.[0-9]+	{ yylval.string = str_dupl(yytext); return NUMFLOAT; }
[0-9]+			{ yylval.string = str_dupl(yytext); return NUMBER; }
\-[0-9]+		{ yylval.string = str_dupl(yytext); return NUMBER; }
\@[0-9a-fA-F]+\@	{ yylval.string = str_dupl(yytext); return TSTAMP; }

[a-zA-Z]+[a-zA-Z0-9\.\-]*	{ yylval.string = str_dupl(yytext); return WORD; }
\"[^"\n]*["\n]		{ yylval.string = str_dupl(yytext); return QUOTEDSTRING; }

\'			{ return SINGLEQUOTE; }
\<=			{ return LESSEQ; }
\>=			{ return GREATEREQ; }
\<			{ return LESS; }
\>			{ return GREATER; }
=			{ return EQUALS; }
,			{ return COMMA; }
\*			{ return STAR; }
;			{ return SEMICOLON; }
\(			{ return OPENBRKT; }
\)			{ return CLOSEBRKT;}
\[			{ return OPENSQBRKT;}
\]			{ return CLOSESQBRKT;}
"//".*\n		/* ignore comments */
\n			/* ignore EOL */
[ \t]+			/* ignore whitespace */
%%
