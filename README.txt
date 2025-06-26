================================================================================
ASSINADOR DIGITAL - README
================================================================================

1. SOBRE O PROGRAMA
-------------------
Este programa permite a assinatura digital de documentos PDF, recorrendo a certificados digitais armazenados localmente no computador, nomeadamente na Microsoft Certificate Store ("Windows-MY"). A aplicação oferece uma interface gráfica intuitiva para seleção de ficheiros, pré-visualização, escolha do certificado e aplicação da assinatura digital, suportando também a assinatura com o Cartão de Cidadão.

2. REQUISITOS
-------------
- Sistema Operativo: Windows 10 ou superior
- Java 8 ou superior
- Permissões de leitura/escrita em ficheiros PDF
- (Opcional) Certificado digital instalado na Microsoft Certificate Store ("Windows-MY")
- (Opcional) Leitor de Cartão de Cidadão, para poder utilizar a assinatura digital certificada

3. EXECUÇÃO DO PROGRAMA
-------------
1. Caso não tenha o Java 8 ou superior instalado no Computador, certifique-se que tem a pasta jre no mesmo local que o ficheiro "assinador.exe"
2. Execute o ficheiro "assinador.exe"
3. Escolha a forma de assinatura que pretende utilizar
4. Caso pretenda testar a assinatura com certificados digitais locais e não tenha nenhum, crie um para testes, como se explica abaixo


4. FUNCIONALIDADES PRINCIPAIS
-----------------------------
- Seleção e listagem de ficheiros PDF
- Pré-visualização de documentos e navegação entre páginas
- Definição visual da área de assinatura
- Seleção de certificado digital a partir da Microsoft Certificate Store
- Assinatura digital de documentos PDF (PAdES)

5. COMO CRIAR UM CERTIFICADO LOCAL DE TESTE
-------------------------------------------
Se não possui um certificado digital instalado no seu computador, pode criar um certificado de teste para experimentar a aplicação. Siga os passos abaixo:

A) Abrir o PowerShell como Administrador
- Clique com o botão direito no menu Iniciar e selecione "Windows PowerShell (Admin)" ou "Prompt de Comando (Admin)".

B) Executar o comando para criar um certificado de teste
- No PowerShell, execute o seguinte comando (tudo numa linha):

New-SelfSignedCertificate -CertStoreLocation Cert:\CurrentUser\My -Subject "CN=CertificadoTesteAssinador"


- Este comando cria um certificado autoassinado no repositório pessoal do utilizador ("Windows-MY") com o nome "CertificadoTesteAssinador".

C) Verificar o certificado
- Abra o utilitário de certificados do Windows:
  1. Pressione `Win + R`, escreva `certmgr.msc` e pressione Enter.
  2. Navegue até "Pessoal" > "Certificados".
  3. Confirme que o certificado "CertificadoTesteAssinador" aparece na lista.

D) Utilizar o certificado na aplicação
- Ao iniciar a aplicação, o certificado de teste aparecerá na listagem de certificados disponíveis para assinatura.
- Pode seleciona-lo para assinar documentos PDF de teste.

Nota: Certificados autoassinados não têm validade legal, mas são suficientes para testar todas as funcionalidades da aplicação.

================================================================================