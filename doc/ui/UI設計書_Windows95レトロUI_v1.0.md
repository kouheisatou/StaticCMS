# Windows95レトロUI設計書

## 文書情報
- **機能ID**: UI-001
- **機能名**: Windows95風レトロUIシステム
- **作成日**: 2024-12-19
- **作成者**: AI Assistant
- **関連ファイル**: 
  - テーマ定義: [RetroTheme.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/ui/theme/RetroTheme.kt)
  - UIコンポーネント: [RetroComponents.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/ui/components/RetroComponents.kt)

## 変更履歴
| バージョン | 日付 | 変更種別 | 変更内容 | 作成者 |
|-----|---|----|----|-----|
| 1.0.0 | 2024-12-19 | 新規作成 | 初版作成 | AI Assistant |

## 1. 機能概要

### 1.1 機能の目的
StaticCMSにWindows95時代の懐かしいUIデザインを再現し、レトロな雰囲気とユーザビリティを両立させる。

### 1.2 機能の範囲
- Windows95風カラーパレット
- 3D効果のあるボーダーとボタン
- レトロフォントスタイル
- ピクセル調のUI要素
- ビンテージウィンドウスタイル

### 1.3 前提条件
- Compose Desktop環境
- モノスペースフォント対応
- RGB カラー表現

## 2. デザインシステム

### 2.1 カラーパレット

#### 基本カラー定義
```kotlin
object RetroColors {
    val Background = Color(0xFFC0C0C0)          // システム背景
    val WindowBackground = Color(0xFFC0C0C0)    // ウィンドウ背景
    val ButtonFace = Color(0xFFC0C0C0)          // ボタン表面
    val ButtonShadow = Color(0xFF808080)        // ボタン影
    val ButtonDarkShadow = Color(0xFF000000)    // ボタン濃い影
    val ButtonHighlight = Color(0xFFDFDFDF)     // ボタンハイライト
    val ButtonLight = Color(0xFFFFFFFF)         // ボタン明るい部分
    val WindowText = Color(0xFF000000)          // テキスト
    val TitleBarActive = Color(0xFF000080)      // アクティブタイトルバー
    val TitleBarText = Color(0xFFFFFFFF)        // タイトルバーテキスト
    val SelectedBackground = Color(0xFF000080)  // 選択背景
    val SelectedText = Color(0xFFFFFFFF)        // 選択テキスト
    val DisabledText = Color(0xFF808080)        // 無効テキスト
}
```

#### カラー使用ガイドライン
- **Background (0xFFC0C0C0)**: アプリケーション全体の背景色
- **ButtonFace (0xFFC0C0C0)**: ボタン、パネルの基本色
- **WindowText (0xFF000000)**: 通常テキストの色
- **TitleBarActive (0xFF000080)**: アクティブ状態の青色
- **DisabledText (0xFF808080)**: 無効状態のグレー

### 2.2 タイポグラフィ

#### フォント定義
```kotlin
object RetroTypography {
    val Default = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = RetroColors.WindowText
    )
    
    val Title = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = RetroColors.TitleBarText
    )
    
    val Button = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = RetroColors.WindowText
    )
}
```

#### タイポグラフィガイドライン
- **フォントファミリー**: モノスペースフォント必須
- **基本サイズ**: 11sp（Windows95の標準サイズ）
- **太字**: タイトルとヘッダーにのみ使用
- **色**: コンテキストに応じてRetroColorsから選択

### 2.3 3D効果システム

#### ボーダー効果の仕組み
```
Raised (通常状態):
┌─ ButtonLight
│┌─ ButtonHighlight
││
││           ─┐ ButtonShadow
││            ─┘ ButtonDarkShadow

Pressed (押下状態):
┌─ ButtonDarkShadow
│┌─ ButtonShadow
││
││
││
```

#### 実装パターン
- **Raised**: 浮き出し効果（通常のボタン状態）
- **Pressed**: 押し込み効果（クリック時やアクティブ状態）
- **Flat**: フラット効果（無効状態）

## 3. UIコンポーネント設計

### 3.1 基本コンポーネント

#### Retro3DBorder
```kotlin
@Composable
fun Retro3DBorder(
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    content: @Composable BoxScope.() -> Unit
)
```

**機能**: 3D効果のボーダーを提供
**用途**: すべてのレトロUIコンポーネントの基盤
**特徴**: 
- 押下状態の切り替え
- ピクセル単位の精密な線描画
- Windows95風の立体感

#### RetroButton
```kotlin
@Composable
fun RetroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
)
```

**機能**: レトロスタイルのボタン
**特徴**:
- クリック時の押し込み効果
- 無効状態のビジュアル変更
- 子要素の柔軟な配置

#### RetroTextField
```kotlin
@Composable
fun RetroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true
)
```

**機能**: レトロスタイルのテキスト入力フィールド
**特徴**:
- 白背景のインセット効果
- プレースホルダーテキスト
- 単行・複行対応

### 3.2 複合コンポーネント

#### RetroWindow
```kotlin
@Composable
fun RetroWindow(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```

**機能**: Windows95風のウィンドウ
**構成要素**:
- タイトルバー（青背景）
- コンテンツエリア（グレー背景）
- 3Dボーダー効果

#### RetroTab
```kotlin
@Composable
fun RetroTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**機能**: レトロスタイルのタブ
**特徴**:
- 選択状態による3D効果の切り替え
- ブラウザタブ風の外観

#### RetroTable
```kotlin
@Composable
fun RetroTable(
    headers: List<String>,
    rows: List<List<String>>,
    onCellClick: (rowIndex: Int, colIndex: Int) -> Unit,
    modifier: Modifier = Modifier
)
```

**機能**: Excel風のレトロテーブル
**特徴**:
- ヘッダー行の強調表示
- セル単位のクリック処理
- ボーダーによるセル区切り

#### RetroProgressBar
```kotlin
@Composable
fun RetroProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
)
```

**機能**: レトロスタイルのプログレスバー
**特徴**:
- インセット効果のある枠
- 青色の進捗表示
- パーセンテージ表示

## 4. 画面別UI設計

### 4.1 RepositoryInputScreen

#### 画面構成
```
┌─────────── StaticCMS - Repository Selection ──────────┐
│                                                        │
│                Welcome to StaticCMS                   │
│                                                        │
│        Git Repository URL:                             │
│        [___________________________________]           │
│        [    Clone Repository    ]                      │
│                                                        │
│        ────────────────────────                       │
│                                                        │
│        Or select a local directory:                    │
│        [  Browse Local Directory  ]                    │
│                                                        │
│        Select a repository containing 'contents'       │
│                                                        │
└────────────────────────────────────────────────────────┘
```

#### UIコンポーネント使用
- `RetroWindow`: 外枠ウィンドウ
- `RetroTextField`: URL入力フィールド
- `RetroTextButton`: Clone、Browse ボタン

### 4.2 CloneProgressScreen

#### 画面構成
```
┌────────── StaticCMS - Cloning Repository ──────────────┐
│                                                        │
│                Cloning Repository...                   │
│              https://github.com/repo.git              │
│                                                        │
│        [████████████████████████]                     │
│                     85%                               │
│                                                        │
│              Resolving deltas...                       │
│                                                        │
└────────────────────────────────────────────────────────┘
```

#### UIコンポーネント使用
- `RetroWindow`: 外枠ウィンドウ
- `RetroProgressBar`: プログレスバー
- `Text`: ステータステキスト

### 4.3 MainScreen

#### 画面構成
```
┌─────────── StaticCMS - Content Manager ──────────────┐
│ [lecture] [member] [news] [tags] [theme]              │
├────────────────────────────────────────────────────────┤
│ Directory: lecture                    Type: Article    │
│                                                        │
│ ┌─────┬────────────┬────────────┬───────────┬──────┐   │
│ │ ID  │ Name (JA)  │ Name (EN)  │ Thumbnail │ ...  │   │
│ ├─────┼────────────┼────────────┼───────────┼──────┤   │
│ │  1  │ AI研究最前線│ Frontiers  │ 1.jpg     │ ... │   │
│ │  3  │ fe         │ fwe        │ 3.png     │ ... │   │
│ └─────┴────────────┴────────────┴───────────┴──────┘   │
│                                                        │
│ Click on an ID to edit the article details            │
└────────────────────────────────────────────────────────┘
```

#### UIコンポーネント使用
- `RetroWindow`: 外枠ウィンドウ
- `RetroTab`: ディレクトリタブ
- `RetroTable`: CSVデータテーブル

### 4.4 ArticleDetailScreen

#### 画面構成
```
┌───────── StaticCMS - Article Editor (ID: 1) ──────────┐
│ [< Back]                                    [Save]     │
├────────────────────────────────────────────────────────┤
│ Markdown Editor               │ Preview                │
│ ┌───────────────────────────┐ │ ┌───────────────────┐  │
│ │ # Sample Article          │ │ │ === Sample Artic  │  │
│ │                           │ │ │                   │  │
│ │ This is a sample...       │ │ │ This is a sample  │  │
│ │                           │ │ │                   │  │
│ │ ![image](./media/img.png) │ │ │ [IMAGE: image]    │  │
│ │                           │ │ │                   │  │
│ │                           │ │ │                   │  │
│ └───────────────────────────┘ │ └───────────────────┘  │
└────────────────────────────────────────────────────────┘
```

#### UIコンポーネント使用
- `RetroWindow`: 外枠ウィンドウ
- `RetroTextButton`: Back、Save ボタン
- `BasicTextField`: Markdown エディタ
- カスタム分割レイアウト

## 5. レスポンシブ設計

### 5.1 ウィンドウサイズ対応
- **最小サイズ**: 800x600px
- **推奨サイズ**: 1024x768px
- **最大サイズ**: 制限なし

### 5.2 レイアウト調整
- **水平分割**: エディタ画面の左右比率調整
- **テーブル**: 水平・垂直スクロール対応
- **タブ**: 水平スクロール対応

## 6. アクセシビリティ

### 6.1 色覚対応
- **コントラスト比**: WCAG AA準拠
- **色以外の識別**: 形状、テキストでの識別
- **ハイコントラスト**: システム設定対応

### 6.2 キーボード操作
- **Tab順序**: 論理的なフォーカス移動
- **ショートカット**: Ctrl+S（保存）等
- **Enter/Space**: ボタン操作

## 7. パフォーマンス考慮事項

### 7.1 描画最適化
- **3D効果**: drawBehindによる効率的な描画
- **状態変更**: 必要最小限の再描画
- **大量データ**: 仮想化による最適化

### 7.2 メモリ使用量
- **カラー定義**: 静的オブジェクトで共有
- **フォント**: システムフォント利用
- **画像**: 遅延読み込み

## 8. 実装ガイドライン

### 8.1 新規コンポーネント作成時
1. **基本構造**: Retro3DBorderをベースとする
2. **カラー**: RetroColorsから選択
3. **フォント**: RetroTypographyを使用
4. **状態管理**: rememberを適切に使用

### 8.2 既存コンポーネント修正時
1. **互換性**: 既存の使用箇所への影響確認
2. **統一性**: 他のレトロコンポーネントとの一貫性
3. **テスト**: 各状態での表示確認

### 8.3 テーマ拡張時
1. **カラー追加**: RetroColors に新しい定義追加
2. **フォント追加**: RetroTypography に新しいスタイル追加
3. **効果追加**: 新しい3D効果パターン定義

## 9. 品質基準

### 9.1 ビジュアル品質
- **ピクセル精度**: 1px単位での正確な描画
- **色再現**: Windows95カラーパレットの忠実な再現
- **立体感**: 適切な光源設定による3D効果

### 9.2 ユーザビリティ
- **直感性**: Windows95ユーザーにとって自然な操作感
- **一貫性**: 全画面での統一されたUI体験
- **フィードバック**: 操作に対する適切な視覚的フィードバック

### 9.3 技術品質
- **可読性**: コンポーネントコードの理解しやすさ
- **再利用性**: 他の画面での利用可能性
- **拡張性**: 新機能追加時の対応容易性

## 関連ファイル
- テーマ定義: [RetroTheme.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/ui/theme/RetroTheme.kt)
- UIコンポーネント: [RetroComponents.kt](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/ui/components/RetroComponents.kt)
- 画面実装: [screens/](../../composeApp/src/desktopMain/kotlin/io/github/kouheisatou/static_cms/screens/) 