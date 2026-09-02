# VeriVoice Frontend

Professional React + TypeScript frontend for the VeriVoice invoice verification platform.

## Setup

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

## Features

- 📄 **Invoice Upload & Verification** - Upload and verify documents with AI
- 🏢 **Vendor Database** - Manage and track verified vendors
- 🔗 **3-Way Matching** - Validate PO, GR, and Invoice alignment
- 📊 **Dashboard** - Real-time verification statistics
- 🎨 **Professional UI** - Light-themed, business-class design

## Tech Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool
- **Tailwind CSS** - Styling
- **React Router** - Navigation
- **Axios** - HTTP client
- **Lucide Icons** - Icons

## API Integration

The frontend connects to the backend API through the `/api` proxy:

- `POST /api/documents/upload` - Upload document
- `GET /api/documents/{id}` - Get document details
- `GET /api/dashboard/stats` - Live dashboard statistics
- `GET /api/erp/purchase-orders` - List POs
- `GET /api/erp/goods-receipts` - List GRs

## Project Structure

```
src/
├── components/       # React components
├── pages/           # Page components
├── services/        # API service layer
├── types/           # TypeScript types
├── utils/           # Helper functions
├── App.tsx          # Main app component
└── main.tsx         # Entry point
```
