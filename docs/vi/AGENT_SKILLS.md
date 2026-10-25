# Agent Skills của dự án

Ngày kiểm tra: 25/10/2026. Chỉ cài tại `.claude/skills/`, không cài global, hooks, MCP server hay plugin runtime. Commit và SHA-256 từng file nằm trong [skills.lock.json](../../.claude/skills.lock.json). Đây là tài liệu setup, không thay PRD/Jira.

## Nguồn, lệnh cài tham khảo và kết quả an toàn

Lệnh dưới đây là lệnh upstream tham khảo, **không phải lệnh đã chạy**. Bản thực tế được clone vào thư mục tạm với Git hooks tắt, đọc source rồi chép đúng thư mục skill tại commit trong lock. Không dùng `npx` vì bước tải/chạy installer chưa được audit; cách chép cũng tránh installer tự thêm agent/global links. Không chạy script upstream trong giai đoạn kiểm tra.

| Yêu cầu | Nguồn chính thức / lệnh tham khảo | Kết quả |
|---|---|---|
| Superpowers | [obra/superpowers](https://github.com/obra/superpowers), `npx skills add obra/superpowers --skill writing-plans --skill test-driven-development --skill verification-before-completion -a claude-code` | Cài 3 skill quy trình; đọc đầy đủ SKILL.md; không có script trong ba thư mục. Không cài nguyên bộ. Commit `2ce8aa15d05bd57213e6f746f953aafb7ded7a8b`. |
| Karpathy | [multica-ai/andrej-karpathy-skills](https://github.com/multica-ai/andrej-karpathy-skills), URL forrestchang chuyển hướng về đây; `npx skills add https://github.com/forrestchang/andrej-karpathy-skills --skill karpathy-guidelines -a claude-code` | Đạt; chỉ hướng dẫn, không script. Commit `2c606141936f1eeef17fa3043a72095b4765b9c2`. Không phải repo do Karpathy trực tiếp duy trì. |
| systematic-debugging | [obra/superpowers](https://github.com/obra/superpowers), `npx skills add obra/superpowers --skill systematic-debugging -a claude-code` | Từ chối bản đã kiểm tra: SKILL.md có ví dụ `printenv IDENTITY`, `security list-keychains`, `security find-identity`. Script find-polluter.sh không có tải mã, nhưng nội dung hướng dẫn không đạt giới hạn môi trường/khóa do người dùng đặt. |
| github-code-review | [ruvnet/ruflo](https://github.com/ruvnet/ruflo), repo claude-flow chuyển hướng; `npx skills add ruvnet/ruflo --skill github-code-review -a claude-code` | Từ chối: hướng dẫn đọc GITHUB_TOKEN, truyền diff sang npx ruv-swarm, tự đăng bình luận/approve/merge và mẫu execSync từ webhook comment. Không cài runtime hay chạy ví dụ. |
| static-analysis | [trailofbits/skills](https://github.com/trailofbits/skills), upstream `/plugin install trailofbits/skills/plugins/static-analysis` | Từ chối bộ hiện tại: đây là plugin gồm codeql/semgrep/sarif-parsing, không có một SKILL.md tên static-analysis. run-scans.sh dùng eval và rm -rf ở thư mục output/tạm bên ngoài skill; generate_suite.sh đọc biến môi trường. Dừng audit khi xác định vi phạm; không tuyên bố đã audit toàn bộ script còn lại. Không cài các skill bảo mật phụ ngoài danh sách. |
| webapp-testing | [anthropics/skills](https://github.com/anthropics/skills), `npx skills add anthropics/skills --skill webapp-testing -a claude-code` | Đạt; đã đọc SKILL.md, with_server.py và ba example Python. Helper chạy lệnh do người gọi cung cấp và kết nối localhost; không tự tải mã/đọc secret. Commit `dbd4588f9e1033efb41dad4bef2f7947c8993d44`. |
| supabase-postgres-best-practices | [supabase/agent-skills](https://github.com/supabase/agent-skills), `npx skills add supabase/agent-skills --skill supabase-postgres-best-practices -a claude-code` | Đạt; SKILL.md và SQL tham khảo, không script. Commit `c9be0e931b7930f7d02126d04774d904c381e7d7`. |
| vercel-react-best-practices | [vercel-labs/agent-skills](https://github.com/vercel-labs/agent-skills), `npx skills add vercel-labs/agent-skills --skill vercel-react-best-practices -a claude-code` | Đạt; nguồn ở skills/react-best-practices, name trong manifest là vercel-react-best-practices; không script. Commit `063bee94c3f4df8453406c830b0a7df0f2860278`. |
| frontend-design | [anthropics/skills](https://github.com/anthropics/skills), `npx skills add anthropics/skills --skill frontend-design -a claude-code` | Đạt; chỉ hướng dẫn thiết kế. Commit `dbd4588f9e1033efb41dad4bef2f7947c8993d44`. |
| skill-creator | [openai/skills](https://github.com/openai/skills), `npx skills add openai/skills --skill skill-creator -a claude-code` | Đạt bản skills/.system/skill-creator: đọc SKILL.md và ba script init_skill.py/generate_openai_yaml.py/quick_validate.py; chỉ tạo/kiểm tra file local. Commit `49f948faa9258a0c61caceaf225e179651397431`. Bản Anthropic bị loại vì improve_description.py đọc os.environ và gửi nội dung qua claude -p. |

Các nguồn được chọn từ tổ chức/tác giả gốc của bộ skill, có hoạt động trên GitHub; metadata thời điểm tải: obra 304k stars, Anthropic 183k, Vercel 32k, Supabase 2.9k, Trail of Bits 7.7k, OpenAI 28k. Đây là tín hiệu nguồn, không thay kiểm tra an toàn, không phải số liệu cập nhật tự động.

## Cây đã cài và kiểm chứng

```text
.claude/
  skills.lock.json
  skills/
    writing-plans/SKILL.md
    test-driven-development/SKILL.md
    verification-before-completion/SKILL.md
    karpathy-guidelines/SKILL.md
    webapp-testing/SKILL.md (+ scripts, examples)
    supabase-postgres-best-practices/SKILL.md (+ references)
    vercel-react-best-practices/SKILL.md (+ rules, AGENTS.md)
    frontend-design/SKILL.md
    skill-creator/SKILL.md (+ scripts, references, agents)
```

Đã kiểm tra tồn tại 9 thư mục và frontmatter có name/description. File hash được ghi khi chép. Chưa khởi động lại Claude Code/phiên Codex để xác nhận tự nhận diện: phiên đang chạy không tự chứng minh khả năng discovery của `.claude/skills/`. Thư mục này dành cho Claude Code; Codex có thể cần cơ chế discovery riêng. Không thêm symlink/global để vượt phạm vi.

## Ngữ cảnh và xung đột

- Karpathy là nguyên tắc thay đổi tối thiểu; writing-plans dùng khi cần kế hoạch nhiều bước; TDD dùng cho thay đổi nghiệp vụ; verification dùng trước báo hoàn thành. Không nạp cả bộ cho câu hỏi đơn giản. Không tự áp dụng yêu cầu xóa code đang có của TDD lên source người dùng.
- Superpowers là tập con, không có namespace plugin `superpowers:` hay tự cài dependency được nhắc trong prose. References tới executing-plans/subagents/finishing-branch không có nghĩa các skill đó đã cài. Chưa có systematic-debugging đạt chính sách; dùng quy trình điều tra thủ công cho đến khi nguồn phù hợp được duyệt.
- React skill chứa cả Next.js/SSR/RSC; dự án là React/Vite nên bỏ qua phần Next.js, không thêm next/dynamic hay Server Components. Description khá rộng, chỉ kích hoạt cho việc React thực sự liên quan.
- PostgreSQL skill chỉ tham khảo SQL/schema/index/transaction. Không suy ra dự án dùng Supabase Auth, RLS, cloud hoặc SDK. Description upstream rất rộng; chỉ áp dụng khi sửa PostgreSQL.
- frontend-design trùng skill cùng tên đã có trong môi trường: chọn bản dự án khi dùng Claude Code; không phối hai chỉ dẫn thẩm mỹ. Giữ layout/Arial đang có khi sửa demo, không tự thiết kế lại.
- webapp-testing hướng dẫn chạy helper black-box; chính sách audit của người dùng có ưu tiên nên đã đọc script trước. Không chạy helper với lệnh chưa hiểu; polling 5 giây làm networkidle không luôn phù hợp.
- skill-creator của OpenAI nhắc Codex; dùng phần chuẩn Agent Skills, không coi agents/openai.yaml là cấu hình Claude Code. Chỉ kích hoạt khi tạo/sửa skill.

## Cập nhật / gỡ an toàn

Với **từng skill**, tra repo/path/commit trong lock. Clone vào thư mục tạm, checkout commit mong muốn, đọc toàn bộ SKILL.md và mọi script, so diff, rồi mới chép thay đúng thư mục `.claude/skills/<name>`. Không tự chạy `npx ... --all`, không tải update vào thư mục đã được tin cậy trước audit. Cập nhật commit + hash trong lock sau khi kiểm tra; giữ license upstream. Không ghi nguồn tạm hoặc secret vào lock.

Gỡ một skill: xác minh đường dẫn tuyệt đối nằm dưới `<repo>/.claude/skills/`, xóa đúng thư mục tên trong cây trên bằng Remove-Item -LiteralPath; bỏ đúng entry khỏi lock. Không xóa `.agents/skills`, global skills, backend hoặc web-admin. Skills bị từ chối chưa tạo thư mục nên không có gì để gỡ.

Để kiểm tra manifest mà không chạy script upstream: đọc mỗi SKILL.md, parse YAML an toàn, xác nhận name/description là chuỗi không rỗng; so SHA-256 từng file với lock. Không có khóa API/token/cấu hình cá nhân trong gói cài.
