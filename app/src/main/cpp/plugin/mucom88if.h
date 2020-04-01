
//
//	MUCOM88 plugin interface structures
//
#ifndef __CMucom88IF_h
#define __CMucom88IF_h

/*------------------------------------------------------------*/
//	for MUCOM88Win interface
/*------------------------------------------------------------*/

//		ファンクション型
//
typedef int (*MUCOM88IF_COMMAND) (void *,int,int,int,void *,void *);
typedef int(*MUCOM88IF_CALLBACK)(void *, int, void *, void *);

typedef int(*MUCOM88IF_STARTUP)(void *, int);


#define MUCOM88IF_VERSION	0x100		// 1.0

#define MUCOM88IF_TYPE_NONE 0
#define MUCOM88IF_TYPE_SILENT 1			// 内部的に動作するもの(デフォルト)
#define MUCOM88IF_TYPE_TOOL 2			// 起動させるタイプの外部ツール

//	if_noticeで通知されるコード
#define	MUCOM88IF_NOTICE_NONE 0
#define	MUCOM88IF_NOTICE_BOOT 0x1000		// 最初の初期化時(起動時のみ)
#define	MUCOM88IF_NOTICE_TERMINATE 0x1001	// 終了(解放)時
#define MUCOM88IF_NOTICE_INTDONE 0x1002		// 演奏割り込み(演奏ルーチン実行)後
#define MUCOM88IF_NOTICE_SESSION 0x1003		// エディタのセッション開始後
#define	MUCOM88IF_NOTICE_RESET 1			// VMリセット時(コンパイル、演奏開始時)
#define MUCOM88IF_NOTICE_DRVINT 2			// VMドライバ実行時(割り込みタイミング)
#define MUCOM88IF_NOTICE_TOOLSTART 3		// プラグインツール起動リクエスト
#define MUCOM88IF_NOTICE_PREPLAY 4			// 演奏開始直前(MUB読み込み直後)
#define MUCOM88IF_NOTICE_PLAY 5				// 演奏開始
#define MUCOM88IF_NOTICE_STOP 6				// 演奏停止
#define MUCOM88IF_NOTICE_MMLSEND 7			// コンパイルMML確定時
#define MUCOM88IF_NOTICE_COMPEND 8			// コンパイル終了後
#define MUCOM88IF_NOTICE_LOADMUB 9			// MUB読み込み後
#define MUCOM88IF_NOTICE_TOOLHIDE 10		// プラグインツール非表示リクエスト

//	if_editorで使用するコマンド
#define	MUCOM88IF_EDITOR_CMD_NONE 0
#define	MUCOM88IF_EDITOR_CMD_PASTECLIP 1	// エディタにクリップボードテキストを貼り付け
#define	MUCOM88IF_EDITOR_CMD_GETTEXTSIZE 2	// エディタのテキストサイズを取得
#define	MUCOM88IF_EDITOR_CMD_GETTEXT 3		// エディタのテキストを取得
#define	MUCOM88IF_EDITOR_CMD_UPDATETEXT 4	// エディタのテキストを更新
#define	MUCOM88IF_EDITOR_CMD_GETCURSOR 5	// エディタのカーソル位置を取得
#define	MUCOM88IF_EDITOR_CMD_SETCURSOR 6	// エディタのカーソル位置を変更

//	if_mucomvmで使用するコマンド
#define	MUCOM88IF_MUCOMVM_CMD_NONE 0
#define	MUCOM88IF_MUCOMVM_CMD_FMWRITE 1			// FMレジスタに書き込み
#define	MUCOM88IF_MUCOMVM_CMD_FMREAD 2			// FMレジスタのテーブルを取得
#define	MUCOM88IF_MUCOMVM_CMD_GETCHDATA 3		// chの演奏データを取得
#define	MUCOM88IF_MUCOMVM_CMD_CHDATA 4			// chの演奏データを取得
#define	MUCOM88IF_MUCOMVM_CMD_TAGDATA 5			// TAGデータを取得
#define	MUCOM88IF_MUCOMVM_CMD_VOICEUPDATE 6		// 音色データを更新
#define	MUCOM88IF_MUCOMVM_CMD_VOICESAVE 7		// 音色データファイルを保存
#define	MUCOM88IF_MUCOMVM_CMD_GETVOICENUM 8		// 音色番号を取得
#define	MUCOM88IF_MUCOMVM_CMD_GETVOICEDATA 9	// 音色データを取得
#define	MUCOM88IF_MUCOMVM_CMD_GETVOICENAME 10	// 音色ファイル名を取得
#define	MUCOM88IF_MUCOMVM_CMD_GETVMMEMMAP 11	// VMのZ80メモリマップを取得

class mucomvm;
class CMucom;

#define MUCOM88IF_FILENAME_MAX 32

class Mucom88Plugin {
public:
	Mucom88Plugin();
	~Mucom88Plugin();

	//	あらかじめ設定される情報
	//
	void *hwnd;							// メインウィンドウハンドル
	int version;						// MUCOM88IFバージョン
	char filename[MUCOM88IF_FILENAME_MAX];		// プラグインファイル名
	void *instance;								// DLLインスタンス

	//	Memory Data structure
	//	(*) = DLL側で書き換え可
	//
	int	type;							// プラグインタイプ(*)
	const char *info;				// プラグイン情報テキストのポインタ(*)

	//	コールバックファンクション(プラグインが設定します)
	MUCOM88IF_CALLBACK if_notice;		// コマンド通知(*)

	//	汎用ファンクション(自動設定されます)
	//
	MUCOM88IF_COMMAND if_mucomvm;	// MUCOM88 VMのアクセス
	MUCOM88IF_COMMAND if_editor;	// エディタ系のサービス

	//	クラス情報 (バージョンで内容変更の可能性があります)
	mucomvm *vm;
	CMucom *mucom;

private:

};

#endif
