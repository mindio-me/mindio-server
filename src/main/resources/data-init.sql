-- ============================================
-- 初始化数据：项目、服务、资源
-- ============================================
-- 注意：执行前请确保 users 表中至少有一个用户
-- 如果 users 表为空，请先创建用户，然后修改下面的 @owner_id 变量

-- 设置默认用户ID（请根据实际情况修改）
SET @owner_id = (SELECT id FROM users LIMIT 1);

-- 如果 @owner_id 为 NULL，请手动设置：
-- SET @owner_id = 1;

-- ============================================
-- 1. 项目数据 (Projects)
-- ============================================
INSERT INTO projects (
    name, subtitle, description, icon, image_url, project_url, github_url, 
    category, technologies, owner_id, is_public, is_featured, display_order,
    created_at, modified_at
) VALUES
-- TrainAdmin
(
    'TrainAdmin',
    'SaaS for dance training institutions',
    'A comprehensive SaaS platform designed specifically for dance training institutions. Features include student management, class scheduling, payment processing, and performance tracking. Built with modern web technologies to provide a seamless experience for both administrators and students.',
    'el-icon-data-analysis',
    NULL,
    NULL,
    NULL,
    'saas',
    'Vue.js,Node.js,MySQL,Docker',
    @owner_id,
    TRUE,
    TRUE,
    1,
    NOW(),
    NOW()
),
-- LIMS Quality Module
(
    'LIMS Quality Module',
    'Odoo-based system',
    'A quality management module integrated into Odoo ERP system. Provides comprehensive quality control features including sample tracking, test management, compliance reporting, and audit trails. Designed for laboratories and manufacturing facilities requiring strict quality assurance processes.',
    'el-icon-data-line',
    NULL,
    NULL,
    NULL,
    'web',
    'Python,Odoo,PostgreSQL',
    @owner_id,
    TRUE,
    TRUE,
    2,
    NOW(),
    NOW()
),
-- Custom CRMEB Extensions
(
    'Custom CRMEB Extensions',
    'E-commerce customization',
    'Custom extensions and modules for CRMEB e-commerce platform. Includes advanced payment gateways, inventory management enhancements, marketing automation tools, and third-party integrations. Tailored solutions to extend platform capabilities for specific business needs.',
    'el-icon-shopping-cart-2',
    NULL,
    NULL,
    NULL,
    'web',
    'PHP,MySQL,Vue.js',
    @owner_id,
    TRUE,
    FALSE,
    3,
    NOW(),
    NOW()
),
-- MindIO Project
(
    'MindIO Project',
    'Personal productivity tool',
    'A personal note-taking and knowledge management system. Features block-based editing, tag organization, search functionality, and multi-format content support. Built to help individuals organize thoughts, ideas, and information efficiently.',
    'el-icon-notebook-2',
    NULL,
    NULL,
    NULL,
    'web',
    'Vue.js,Nuxt.js,Spring Boot,MySQL',
    @owner_id,
    TRUE,
    FALSE,
    4,
    NOW(),
    NOW()
);

-- ============================================
-- 2. 资源数据 (Resources)
-- ============================================
-- 注意：category 字段是必填的，分类包括：ai-tools, dev-tools, frameworks, learning, design

-- AI-Powered Tools (30个)
INSERT INTO resources (
    name, description, url, icon, category, tags, owner_id, is_public, is_featured, display_order,
    created_at, modified_at
) VALUES
-- Row 1 - Design & Development
(
    'Figma AI',
    'AI-powered design features in Figma for auto-layout, content generation, and smart suggestions.',
    'https://www.figma.com/ai/',
    'el-icon-picture-outline',
    'ai-tools',
    'Design,AI,Prototyping',
    @owner_id,
    TRUE,
    FALSE,
    1,
    NOW(),
    NOW()
),
(
    'Visily',
    'AI-powered wireframe and prototype tool. Transform screenshots into editable designs instantly.',
    'https://visily.ai/',
    'el-icon-brush',
    'ai-tools',
    'Design,AI,Wireframing',
    @owner_id,
    TRUE,
    FALSE,
    2,
    NOW(),
    NOW()
),
(
    'Gemini',
    'Google\'s multimodal AI assistant for text, image, and code generation with advanced reasoning.',
    'https://gemini.google.com/',
    'el-icon-star-off',
    'ai-tools',
    'AI,Assistant,Multimodal',
    @owner_id,
    TRUE,
    TRUE,
    3,
    NOW(),
    NOW()
),
(
    'Cursor',
    'AI-first code editor built for pair programming with AI. Write, edit, and debug code faster.',
    'https://www.cursor.com/',
    'el-icon-edit-outline',
    'ai-tools',
    'AI,Code Editor,Development',
    @owner_id,
    TRUE,
    TRUE,
    4,
    NOW(),
    NOW()
),
(
    'Claude',
    'Anthropic\'s AI assistant excelling at analysis, writing, coding, and thoughtful conversations.',
    'https://claude.ai/',
    'el-icon-chat-dot-round',
    'ai-tools',
    'AI,Assistant,Analysis',
    @owner_id,
    TRUE,
    TRUE,
    5,
    NOW(),
    NOW()
),
(
    'GitHub Copilot',
    'AI pair programmer that suggests code completions and entire functions in your IDE.',
    'https://github.com/features/copilot',
    'el-icon-coin',
    'ai-tools',
    'AI,Code,IDE',
    @owner_id,
    TRUE,
    TRUE,
    6,
    NOW(),
    NOW()
),
-- Row 2 - Creative Generation
(
    'ChatGPT',
    'OpenAI\'s conversational AI for writing, analysis, coding, and creative tasks.',
    'https://chat.openai.com/',
    'el-icon-chat-line-round',
    'ai-tools',
    'AI,Assistant,Chat',
    @owner_id,
    TRUE,
    TRUE,
    7,
    NOW(),
    NOW()
),
(
    'Midjourney',
    'AI art generator creating stunning images from text prompts. Best for artistic visuals.',
    'https://www.midjourney.com/',
    'el-icon-picture',
    'ai-tools',
    'AI,Image Generation,Art',
    @owner_id,
    TRUE,
    FALSE,
    8,
    NOW(),
    NOW()
),
(
    'Stable Diffusion',
    'Open-source AI image generation model. Run locally or via API for creative control.',
    'https://stability.ai/',
    'el-icon-camera',
    'ai-tools',
    'AI,Image Generation,Open Source',
    @owner_id,
    TRUE,
    FALSE,
    9,
    NOW(),
    NOW()
),
(
    'Adobe Firefly',
    'Adobe\'s generative AI for creative workflows. Safe for commercial use with content credentials.',
    'https://www.adobe.com/products/firefly.html',
    'el-icon-magic-stick',
    'ai-tools',
    'AI,Image Generation,Adobe',
    @owner_id,
    TRUE,
    FALSE,
    10,
    NOW(),
    NOW()
),
(
    'Runway',
    'AI video generation and editing platform. Create videos from text, images, or other videos.',
    'https://www.runway.ml/',
    'el-icon-video-camera',
    'ai-tools',
    'AI,Video Generation,Editing',
    @owner_id,
    TRUE,
    FALSE,
    11,
    NOW(),
    NOW()
),
(
    'Perplexity',
    'AI-powered search engine providing answers with citations from real-time web sources.',
    'https://www.perplexity.ai/',
    'el-icon-search',
    'ai-tools',
    'AI,Search,Research',
    @owner_id,
    TRUE,
    FALSE,
    12,
    NOW(),
    NOW()
),
-- Row 3 - Productivity
(
    'v0 by Vercel',
    'AI UI generator that creates React components from text descriptions using shadcn/ui.',
    'https://v0.dev/',
    'el-icon-cpu',
    'ai-tools',
    'AI,UI Generation,React',
    @owner_id,
    TRUE,
    TRUE,
    13,
    NOW(),
    NOW()
),
(
    'Framer AI',
    'Generate and publish websites from text prompts. No-code website builder with AI.',
    'https://www.framer.com/ai',
    'el-icon-files',
    'ai-tools',
    'AI,Website Builder,No-code',
    @owner_id,
    TRUE,
    FALSE,
    14,
    NOW(),
    NOW()
),
(
    'Notion AI',
    'AI writing assistant integrated into Notion for drafting, summarizing, and brainstorming.',
    'https://www.notion.so/product/ai',
    'el-icon-notebook-2',
    'ai-tools',
    'AI,Writing,Productivity',
    @owner_id,
    TRUE,
    FALSE,
    15,
    NOW(),
    NOW()
),
(
    'Jasper',
    'AI content platform for marketing teams. Generate blog posts, ads, and social media content.',
    'https://www.jasper.ai/',
    'el-icon-document',
    'ai-tools',
    'AI,Content,Marketing',
    @owner_id,
    TRUE,
    FALSE,
    16,
    NOW(),
    NOW()
),
(
    'Copy.ai',
    'AI copywriting tool for marketing content, product descriptions, and sales emails.',
    'https://www.copy.ai/',
    'el-icon-edit',
    'ai-tools',
    'AI,Writing,Marketing',
    @owner_id,
    TRUE,
    FALSE,
    17,
    NOW(),
    NOW()
),
(
    'Grammarly',
    'AI writing assistant for grammar, spelling, tone, and clarity across all platforms.',
    'https://grammarly.com/',
    'el-icon-check',
    'ai-tools',
    'AI,Writing,Grammar',
    @owner_id,
    TRUE,
    FALSE,
    18,
    NOW(),
    NOW()
),
-- Row 4 - Media
(
    'Synthesia',
    'Create AI videos with virtual avatars. Generate training videos without cameras or actors.',
    'https://www.synthesia.io/',
    'el-icon-video-play',
    'ai-tools',
    'AI,Video,Avatar',
    @owner_id,
    TRUE,
    FALSE,
    19,
    NOW(),
    NOW()
),
(
    'ElevenLabs',
    'AI voice generation and cloning. Create realistic voiceovers in multiple languages.',
    'https://elevenlabs.io/',
    'el-icon-microphone',
    'ai-tools',
    'AI,Audio,Voice',
    @owner_id,
    TRUE,
    FALSE,
    20,
    NOW(),
    NOW()
),
(
    'Descript',
    'AI-powered audio/video editor. Edit media by editing text transcripts.',
    'https://www.descript.com/',
    'el-icon-headset',
    'ai-tools',
    'AI,Audio,Video,Editing',
    @owner_id,
    TRUE,
    FALSE,
    21,
    NOW(),
    NOW()
),
(
    'Remove.bg',
    'AI background removal tool. Remove image backgrounds instantly with one click.',
    'https://www.remove.bg/',
    'el-icon-scissors',
    'ai-tools',
    'AI,Image,Background Removal',
    @owner_id,
    TRUE,
    FALSE,
    22,
    NOW(),
    NOW()
),
(
    'Canva AI',
    'AI features in Canva including Magic Write, image generation, and design suggestions.',
    'https://www.canva.com/ai-image-generator/',
    'el-icon-picture-outline-round',
    'ai-tools',
    'AI,Design,Image Generation',
    @owner_id,
    TRUE,
    FALSE,
    23,
    NOW(),
    NOW()
),
(
    'Beautiful.ai',
    'AI presentation maker that automatically applies design rules to your slides.',
    'https://beautiful.ai/',
    'el-icon-data-line',
    'ai-tools',
    'AI,Presentation,Design',
    @owner_id,
    TRUE,
    FALSE,
    24,
    NOW(),
    NOW()
),
-- Row 5 - Developer Platforms
(
    'Hugging Face',
    'Open-source AI community and platform. Host models, datasets, and ML applications.',
    'https://www.huggingface.co/',
    'el-icon-present',
    'ai-tools',
    'AI,ML,Open Source',
    @owner_id,
    TRUE,
    TRUE,
    25,
    NOW(),
    NOW()
),
(
    'Replicate',
    'Run open-source AI models via API. Deploy custom models without infrastructure.',
    'https://replicate.com/',
    'el-icon-refresh',
    'ai-tools',
    'AI,API,ML',
    @owner_id,
    TRUE,
    FALSE,
    26,
    NOW(),
    NOW()
),
(
    'Anthropic',
    'AI safety company building Claude. Access APIs for enterprise AI applications.',
    'https://www.anthropic.com/',
    'el-icon-coordinate',
    'ai-tools',
    'AI,API,Enterprise',
    @owner_id,
    TRUE,
    FALSE,
    27,
    NOW(),
    NOW()
),
(
    'Cohere',
    'Enterprise AI platform for NLP. Build search, generation, and classification features.',
    'https://cohere.com/',
    'el-icon-connection',
    'ai-tools',
    'AI,NLP,Enterprise',
    @owner_id,
    TRUE,
    FALSE,
    28,
    NOW(),
    NOW()
),
(
    'LangChain',
    'Framework for building LLM applications. Chain prompts, tools, and memory together.',
    'https://www.langchain.com/',
    'el-icon-link',
    'ai-tools',
    'AI,Framework,LLM',
    @owner_id,
    TRUE,
    TRUE,
    29,
    NOW(),
    NOW()
),
(
    'Databricks',
    'Data and AI platform for building, training, and deploying ML models at scale.',
    'https://www.databricks.com/',
    'el-icon-s-data',
    'ai-tools',
    'AI,ML,Data Platform',
    @owner_id,
    TRUE,
    FALSE,
    30,
    NOW(),
    NOW()
);

-- Development Tools (4个)
INSERT INTO resources (
    name, description, url, icon, category, tags, owner_id, is_public, is_featured, display_order,
    created_at, modified_at
) VALUES
(
    'VS Code',
    'My primary code editor with extensions for Vue.js, ESLint, Prettier, and GitLens.',
    'https://code.visualstudio.com/',
    'el-icon-monitor',
    'dev-tools',
    'Editor,IDE,Development',
    @owner_id,
    TRUE,
    TRUE,
    1,
    NOW(),
    NOW()
),
(
    'Docker',
    'Containerization platform for consistent development and deployment environments.',
    'https://www.docker.com/',
    'el-icon-takeaway-box',
    'dev-tools',
    'Container,Docker,DevOps',
    @owner_id,
    TRUE,
    TRUE,
    2,
    NOW(),
    NOW()
),
(
    'Postman',
    'API development and testing tool for building and documenting RESTful APIs.',
    'https://www.postman.com/',
    'el-icon-connection',
    'dev-tools',
    'API,Testing,Development',
    @owner_id,
    TRUE,
    FALSE,
    3,
    NOW(),
    NOW()
),
(
    'Git',
    'Version control system for tracking changes and collaborating with teams.',
    'https://git-scm.com/',
    'el-icon-setting',
    'dev-tools',
    'Version Control,Git,Development',
    @owner_id,
    TRUE,
    TRUE,
    4,
    NOW(),
    NOW()
);

-- Frameworks & Libraries (4个)
INSERT INTO resources (
    name, description, url, icon, category, tags, owner_id, is_public, is_featured, display_order,
    created_at, modified_at
) VALUES
(
    'Vue.js',
    'Progressive JavaScript framework for building user interfaces and single-page applications.',
    'https://vuejs.org/',
    'el-icon-cpu',
    'frameworks',
    'Framework,JavaScript,Vue',
    @owner_id,
    TRUE,
    TRUE,
    1,
    NOW(),
    NOW()
),
(
    'Node.js',
    'JavaScript runtime built on Chrome\'s V8 engine for building scalable backend services.',
    'https://nodejs.org/',
    'el-icon-coin',
    'frameworks',
    'Runtime,JavaScript,Backend',
    @owner_id,
    TRUE,
    TRUE,
    2,
    NOW(),
    NOW()
),
(
    'Nuxt.js',
    'Vue.js framework for server-side rendering, static site generation, and more.',
    'https://nuxtjs.org/',
    'el-icon-files',
    'frameworks',
    'Framework,Vue,SSR',
    @owner_id,
    TRUE,
    TRUE,
    3,
    NOW(),
    NOW()
),
(
    'Tailwind CSS',
    'Utility-first CSS framework for rapidly building custom user interfaces.',
    'https://tailwindcss.com/',
    'el-icon-brush',
    'frameworks',
    'CSS,Framework,Styling',
    @owner_id,
    TRUE,
    FALSE,
    4,
    NOW(),
    NOW()
);

-- Learning Resources (4个)
INSERT INTO resources (
    name, description, url, icon, category, tags, owner_id, is_public, is_featured, display_order,
    created_at, modified_at
) VALUES
(
    'MDN Web Docs',
    'Comprehensive documentation for web technologies including HTML, CSS, and JavaScript.',
    'https://developer.mozilla.org/',
    'el-icon-reading',
    'learning',
    'Documentation,Learning,Web',
    @owner_id,
    TRUE,
    TRUE,
    1,
    NOW(),
    NOW()
),
(
    'Vue Mastery',
    'Premium video courses and tutorials for mastering Vue.js and the Vue ecosystem.',
    'https://www.vuemastery.com/',
    'el-icon-video-camera',
    'learning',
    'Courses,Vue,Learning',
    @owner_id,
    TRUE,
    FALSE,
    2,
    NOW(),
    NOW()
),
(
    'freeCodeCamp',
    'Free coding bootcamp with thousands of hours of content on web development.',
    'https://www.freecodecamp.org/',
    'el-icon-notebook-1',
    'learning',
    'Courses,Free,Learning',
    @owner_id,
    TRUE,
    TRUE,
    3,
    NOW(),
    NOW()
),
(
    'Awesome Vue',
    'Curated list of awesome things related to Vue.js - libraries, tools, and resources.',
    'https://github.com/vuejs/awesome-vue',
    'el-icon-collection',
    'learning',
    'Resources,Vue,Curated',
    @owner_id,
    TRUE,
    FALSE,
    4,
    NOW(),
    NOW()
);

-- Design & Prototyping (4个)
INSERT INTO resources (
    name, description, url, icon, category, tags, owner_id, is_public, is_featured, display_order,
    created_at, modified_at
) VALUES
(
    'Figma',
    'Collaborative interface design tool for creating UI/UX designs and prototypes.',
    'https://www.figma.com/',
    'el-icon-picture',
    'design',
    'Design,Prototyping,UI/UX',
    @owner_id,
    TRUE,
    TRUE,
    1,
    NOW(),
    NOW()
),
(
    'Unsplash',
    'Free high-resolution photos for commercial and personal use.',
    'https://unsplash.com/',
    'el-icon-picture-outline',
    'design',
    'Images,Free,Stock Photos',
    @owner_id,
    TRUE,
    FALSE,
    2,
    NOW(),
    NOW()
),
(
    'Dribbble',
    'Design inspiration platform showcasing creative work from designers worldwide.',
    'https://dribbble.com/',
    'el-icon-medal',
    'design',
    'Design,Inspiration,Portfolio',
    @owner_id,
    TRUE,
    FALSE,
    3,
    NOW(),
    NOW()
),
(
    'Coolors',
    'Fast color scheme generator for creating beautiful color palettes.',
    'https://coolors.co/',
    'el-icon-magic-stick',
    'design',
    'Design,Colors,Palette',
    @owner_id,
    TRUE,
    FALSE,
    4,
    NOW(),
    NOW()
);

-- ============================================
-- 完成
-- ============================================
-- 数据插入完成！
-- 总计：
-- - 项目 (Projects): 4 条
-- - 资源 (Resources): 46 条
--   - AI Tools: 30 条
--   - Dev Tools: 4 条
--   - Frameworks: 4 条
--   - Learning: 4 条
--   - Design: 4 条
