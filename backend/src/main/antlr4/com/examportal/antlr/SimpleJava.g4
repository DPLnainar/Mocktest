grammar SimpleJava;



// Parser Rules
compilationUnit
    : (packageDeclaration | importDeclaration | typeDeclaration)* EOF
    ;

packageDeclaration
    : 'package' qualifiedName ';'
    ;

importDeclaration
    : 'import' 'static'? qualifiedName ('.' '*')? ';'
    ;

typeDeclaration
    : classDeclaration
    | interfaceDeclaration
    | ';'+
    ;

classDeclaration
    : 'class' IDENTIFIER (typeParameters)? ('extends' typeType)? ('implements' typeList)? classBody
    ;

interfaceDeclaration
    : 'interface' IDENTIFIER (typeParameters)? ('extends' typeList)? interfaceBody
    ;

classBody
    : '{' classBodyDeclaration* '}'
    ;

interfaceBody
    : '{' interfaceBodyDeclaration* '}'
    ;

classBodyDeclaration
    : ';'
    | 'static'? block
    | modifier* memberDeclaration
    ;

interfaceBodyDeclaration
    : modifier* interfaceMemberDeclaration
    | ';'
    ;

memberDeclaration
    : methodDeclaration
    | fieldDeclaration
    | constructorDeclaration
    ;

interfaceMemberDeclaration
    : methodDeclaration
    | typeDeclaration
    ;

methodDeclaration
    : (typeType | 'void') IDENTIFIER formalParameters (throwsClause)? (block | ';')
    ;

constructorDeclaration
    : IDENTIFIER formalParameters (throwsClause)? block
    ;

fieldDeclaration
    : typeType variableDeclarators ';'
    ;

variableDeclarators
    : variableDeclarator (',' variableDeclarator)*
    ;

variableDeclarator
    : IDENTIFIER ('=' variableInitializer)?
    ;

variableInitializer
    : expression
    | arrayInitializer
    ;

arrayInitializer
    : '{' (variableInitializer (',' variableInitializer)*)? ','? '}'
    ;

typeType
    : (classOrInterfaceType | primitiveType) ('[' ']')*
    ;

classOrInterfaceType
    : IDENTIFIER
    ;

primitiveType
    : 'boolean' | 'char' | 'byte' | 'short' | 'int' | 'long' | 'float' | 'double' | 'void'
    ;

formalParameters
    : '(' (formalParameterList)? ')'
    ;

formalParameterList
    : formalParameter (',' formalParameter)*
    ;

formalParameter
    : typeType IDENTIFIER
    ;

throwsClause
    : 'throws' qualifiedName (',' qualifiedName)*
    ;

typeList
    : typeType (',' typeType)*
    ;

block
    : '{' blockStatement* '}'
    ;

blockStatement
    : localVariableDeclaration ';'
    | statement
    ;

localVariableDeclaration
    : typeType variableDeclarators
    ;

statement
    : block                                     # blockStmt
    | 'if' parExpression statement ('else' statement)? # ifStmt
    | 'for' '(' (forControl) ')' statement      # forStatement
    | 'while' parExpression statement           # whileStatement
    | 'do' statement 'while' parExpression ';'  # doWhileStatement
    | 'try' block (catchClause)* (finallyBlock)? # tryCatchStmt
    | 'return' expression? ';'                  # returnStmt
    | 'throw' expression ';'                    # throwStmt
    | 'break' IDENTIFIER? ';'                   # breakStmt
    | 'continue' IDENTIFIER? ';'                # continueStmt
    | expression ';'                            # expressionStmt
    | ';'                                       # emptyStmt
    ;

catchClause
    : 'catch' '(' tryCatchParameter ')' block
    ;

tryCatchParameter
    : typeType IDENTIFIER
    ;

finallyBlock
    : 'finally' block
    ;

forControl
    : (forInit? ';' expression? ';' forUpdate?)
    | (typeType IDENTIFIER ':' expression)
    ;

forInit
    : localVariableDeclaration
    | expressionList
    ;

forUpdate
    : expressionList
    ;

parExpression
    : '(' expression ')'
    ;

expressionList
    : expression (',' expression)*
    ;

expression
    : primary                                   # primaryExpr
    | expression '.' IDENTIFIER '(' expressionList? ')' # methodCall
    | expression '.' IDENTIFIER                 # fieldAccess
    | expression '[' expression ']'             # arrayAccess
    | expression ('++' | '--')                  # postfixExpr
    | ('+' | '-' | '++' | '--' | '!' | '~') expression # prefixExpr
    | expression ('*' | '/' | '%') expression   # multiplicativeExpr
    | expression ('+' | '-') expression         # additiveExpr
    | expression ('<<' | '>>>' | '>>') expression # shiftExpr
    | expression ('<=' | '>=' | '>' | '<') expression # relationalExpr
    | expression 'instanceof' typeType          # instanceOfExpr
    | expression ('==' | '!=') expression       # equalityExpr
    | expression '&' expression                 # bitwiseAndExpr
    | expression '^' expression                 # bitwiseXorExpr
    | expression '|' expression                 # bitwiseOrExpr
    | expression '&&' expression                # logicalAndExpr
    | expression '||' expression                # logicalOrExpr
    | expression '?' expression ':' expression  # ternaryExpr
    | <assoc=right> expression ('=' | '+=' | '-=' | '*=' | '/=' | '&=' | '|=' | '^=' | '>>=' | '>>>=' | '<<=' | '%=') expression # assignmentExpr
    | 'new' creator                             # newExpr
    ;

primary
    : '(' expression ')'
    | 'this'
    | 'super'
    | literal
    | IDENTIFIER
    | typeType '.' 'class'
    | 'void' '.' 'class'
    ;

creator
    : createdName (arrayCreatorRest | classCreatorRest)
    ;

createdName
    : classOrInterfaceType
    | primitiveType
    ;

arrayCreatorRest
    : '[' (']' '[' | expression ']')* arrayInitializer
    | '[' expression ']' ('[' expression ']')* ('[' ']')*
    ;

classCreatorRest
    : '(' expressionList? ')' classBody?
    ;

qualifiedName
    : IDENTIFIER ('.' IDENTIFIER)*
    ;

literal
    : DECIMAL_LITERAL
    | HEX_LITERAL
    | OCT_LITERAL
    | BINARY_LITERAL
    | FLOAT_LITERAL
    | HEX_FLOAT_LITERAL
    | BOOL_LITERAL
    | CHAR_LITERAL
    | STRING_LITERAL
    | 'null'
    ;

modifier
    : 'public' | 'protected' | 'private' | 'static' | 'abstract' | 'final' | 'native' | 'synchronized' | 'transient' | 'volatile' | 'strictfp'
    ;

typeParameters
    : '<' typeParameter (',' typeParameter)* '>'
    ;

typeParameter
    : IDENTIFIER ('extends' typeBound)?
    ;

typeBound
    : typeType ('&' typeType)*
    ;

// Lexer Rules
IDENTIFIER : [a-zA-Z_$] [a-zA-Z0-9_$]* ;

DECIMAL_LITERAL : ('0' | [1-9] [0-9]*) [lL]? ;
HEX_LITERAL : '0' [xX] [0-9a-fA-F]+ [lL]? ;
OCT_LITERAL : '0' [0-7]+ [lL]? ;
BINARY_LITERAL : '0' [bB] [0-1]+ [lL]? ;

FLOAT_LITERAL
    : [0-9]+ '.' [0-9]* ([eE] [+-]? [0-9]+)? [fFdD]?
    | '.' [0-9]+ ([eE] [+-]? [0-9]+)? [fFdD]?
    | [0-9]+ [eE] [+-]? [0-9]+ [fFdD]?
    | [0-9]+ [fFdD]
    ;

HEX_FLOAT_LITERAL : '0' [xX] [0-9a-fA-F]* '.'? [0-9a-fA-F]+ [pP] [+-]? [0-9]+ [fFdD]? ;

BOOL_LITERAL : 'true' | 'false' ;
CHAR_LITERAL : '\'' (~['\\\r\n] | '\\' ['"\\rntb]) '\'' ;
STRING_LITERAL : '"' (~["\\\r\n] | '\\' ['"\\rntb])* '"' ;

WS : [ \t\r\n\u000C]+ -> skip ;
COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
